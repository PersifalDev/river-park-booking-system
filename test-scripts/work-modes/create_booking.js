import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const bookingBaseUrl = __ENV.BOOKING_BASE_URL || 'http://localhost:8084';
const paymentBaseUrl = __ENV.PAYMENT_BASE_URL || 'http://localhost:8087';
const notificationBaseUrl = __ENV.NOTIFICATION_BASE_URL || 'http://localhost:8088';
const token = __ENV.TOKEN || '';
const workMode = (__ENV.WORK_MODE || 'ASYNC').toUpperCase();
const categoryIds = (__ENV.CATEGORY_IDS || '1,2,3')
  .split(',')
  .map((value) => Number(value.trim()))
  .filter((value) => Number.isInteger(value) && value > 0);
const pollIntervalMs = Number(__ENV.POLL_INTERVAL_MS || 250);
const completionTimeoutMs = Number(__ENV.COMPLETION_TIMEOUT_MS || 45000);
const targetRate = Number(__ENV.TARGET_RATE || 0);
const iterations = Number(__ENV.ITERATIONS || 0);
const runSeed = Number(__ENV.RUN_SEED || 1);

const responseLatency = new Trend('booking_response_latency_ms', true);
const holdLatency = new Trend('booking_hold_latency_ms', true);
const businessCompletionLatency = new Trend('business_completion_latency_ms', true);
const notificationCompletionLatency = new Trend('notification_completion_latency_ms', true);
const successfulIterations = new Rate('successful_iterations');
const responseAlreadyHold = new Rate('response_already_hold');
const completionTimeouts = new Counter('completion_timeouts_total');
const bookingFailures = new Counter('booking_failures_total');

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  scenarios: {
    compare_work_modes: targetRate > 0
      ? {
          executor: 'constant-arrival-rate',
          exec: 'createBooking',
          rate: targetRate,
          timeUnit: __ENV.RATE_TIME_UNIT || '1s',
          duration: __ENV.DURATION || '60s',
          preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || Math.max(10, targetRate * 2)),
          maxVUs: Number(__ENV.MAX_VUS || Math.max(50, targetRate * 10)),
          gracefulStop: __ENV.GRACEFUL_STOP || '50s',
          tags: { work_mode: workMode },
        }
      : iterations > 0
      ? {
          executor: 'shared-iterations',
          exec: 'createBooking',
          vus: Number(__ENV.VUS || 1),
          iterations,
          maxDuration: __ENV.MAX_DURATION || '10m',
          tags: { work_mode: workMode },
        }
      : {
          executor: 'constant-vus',
          exec: 'createBooking',
          vus: Number(__ENV.VUS || 10),
          duration: __ENV.DURATION || '60s',
          gracefulStop: __ENV.GRACEFUL_STOP || '50s',
          tags: { work_mode: workMode },
        },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    booking_response_latency_ms: [`p(95)<${__ENV.RESPONSE_P95_MS || 5000}`],
    booking_hold_latency_ms: [`p(95)<${__ENV.HOLD_P95_MS || 30000}`],
    business_completion_latency_ms: [`p(95)<${__ENV.COMPLETION_P95_MS || 45000}`],
    notification_completion_latency_ms: [`p(95)<${__ENV.COMPLETION_P95_MS || 45000}`],
    successful_iterations: ['rate>0.95'],
    completion_timeouts_total: ['count==0'],
  },
};

export function setup() {
  if (!token) {
    throw new Error('TOKEN is required');
  }
  if (categoryIds.length === 0) {
    throw new Error('CATEGORY_IDS must contain at least one positive category id');
  }
  for (const url of [
    `${bookingBaseUrl}/actuator/health`,
    `${paymentBaseUrl}/actuator/health`,
    `${notificationBaseUrl}/actuator/health`,
  ]) {
    const response = http.get(url, { timeout: '5s', tags: { operation: 'health' } });
    if (!check(response, { [`${url} is healthy`]: (value) => value.status === 200 })) {
      throw new Error(`Service is not healthy: ${url}; status=${response.status}`);
    }
  }
}

export function createBooking() {
  const startedAt = Date.now();
  const categoryId = categoryIds[(runSeed + __VU + __ITER) % categoryIds.length];
  const dateShift = 30 + ((runSeed * 17 + __VU * 31 + __ITER) % 180);
  const payload = JSON.stringify({
    categoryId,
    checkInDate: dateFromToday(dateShift),
    checkOutDate: dateFromToday(dateShift + 1),
    guests: 2,
    adultCount: 2,
    childrenCount: 0,
    tariffCode: __ENV.TARIFF_CODE || null,
    promoCode: null,
  });
  const headers = {
    Authorization: `Bearer ${token}`,
    'Content-Type': 'application/json',
    'Idempotency-Key': `k6-work-mode-${workMode}-${runSeed}-${Date.now()}-${__VU}-${__ITER}`,
  };

  const createResponse = http.post(`${bookingBaseUrl}/booking`, payload, {
    headers,
    tags: { operation: 'create_booking', work_mode: workMode },
    timeout: __ENV.HTTP_TIMEOUT || '15s',
  });
  responseLatency.add(createResponse.timings.duration);

  const responseBody = safeJson(createResponse);
  if (!check(createResponse, {
    'booking created': (response) => response.status === 201,
    'booking id returned': () => Boolean(responseBody?.id),
  })) {
    bookingFailures.add(1);
    successfulIterations.add(false);
    return;
  }

  const bookingId = responseBody.id;
  let bookingStatus = responseBody.status;
  responseAlreadyHold.add(bookingStatus === 'HOLD');
  const deadline = Date.now() + completionTimeoutMs;

  while (bookingStatus === 'CREATED' && Date.now() < deadline) {
    sleep(pollIntervalMs / 1000);
    const response = http.get(`${bookingBaseUrl}/booking/${bookingId}`, {
      headers: { Authorization: `Bearer ${token}` },
      tags: { operation: 'poll_booking', work_mode: workMode },
      timeout: __ENV.HTTP_TIMEOUT || '15s',
    });
    if (response.status === 200) {
      bookingStatus = safeJson(response)?.status;
    }
  }

  if (bookingStatus !== 'HOLD') {
    bookingFailures.add(1, { status: bookingStatus || 'unknown' });
    if (Date.now() >= deadline) {
      completionTimeouts.add(1);
    }
    successfulIterations.add(false);
    return;
  }
  holdLatency.add(Date.now() - startedAt);

  let paymentReady = false;
  while (!paymentReady && Date.now() < deadline) {
    const response = http.get(`${paymentBaseUrl}/payments/booking/${bookingId}`, {
      headers: { Authorization: `Bearer ${token}` },
      tags: { operation: 'poll_payment', work_mode: workMode },
      timeout: __ENV.HTTP_TIMEOUT || '15s',
      responseCallback: http.expectedStatuses(200, 404),
    });
    paymentReady = response.status === 200
      && ['PENDING', 'CONFIRMED'].includes(safeJson(response)?.status);
    if (!paymentReady) {
      sleep(pollIntervalMs / 1000);
    }
  }

  if (!paymentReady) {
    completionTimeouts.add(1);
    successfulIterations.add(false);
    return;
  }

  let notificationReady = false;
  while (!notificationReady && Date.now() < deadline) {
    const response = http.get(`${notificationBaseUrl}/notifications?pageNumber=0&pageSize=100`, {
      headers: { Authorization: `Bearer ${token}` },
      tags: { operation: 'poll_notification', work_mode: workMode },
      timeout: __ENV.HTTP_TIMEOUT || '15s',
    });
    if (response.status === 200) {
      const notifications = safeJson(response)?.content || [];
      notificationReady = notifications.some((notification) =>
        notification.bookingId === bookingId
        && ['BOOKING_HOLD_CREATED', 'PAYMENT_PENDING'].includes(notification.type));
    }
    if (!notificationReady) {
      sleep(pollIntervalMs / 1000);
    }
  }

  if (!notificationReady) {
    completionTimeouts.add(1);
    successfulIterations.add(false);
    return;
  }

  notificationCompletionLatency.add(Date.now() - startedAt);
  businessCompletionLatency.add(Date.now() - startedAt);
  const cancelResponse = http.patch(`${bookingBaseUrl}/booking/${bookingId}/cancel`, null, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { operation: 'cancel_booking', work_mode: workMode },
    timeout: __ENV.HTTP_TIMEOUT || '15s',
  });
  const cancelled = check(cancelResponse, {
    'booking cancelled': (response) => response.status === 200,
  });
  successfulIterations.add(cancelled);
}

export default createBooking;

function safeJson(response) {
  try {
    return response.json();
  } catch (_) {
    return null;
  }
}

function dateFromToday(days) {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}
