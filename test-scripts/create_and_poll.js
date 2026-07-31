import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

const baseUrl = __ENV.BASE_URL || 'http://localhost:8084';
const token = __ENV.TOKEN || '';
const mode = __ENV.MODE || 'unspecified';
const categoryIds = (__ENV.CATEGORY_IDS || '1,2,3')
  .split(',')
  .map((value) => Number(value.trim()))
  .filter((value) => Number.isInteger(value) && value > 0);

const pollIntervalMs = Number(__ENV.POLL_INTERVAL_MS || 250);
const terminalTimeoutMs = Number(__ENV.TERMINAL_TIMEOUT_MS || 30000);
const dateOffsetDays = Number(__ENV.DATE_OFFSET_DAYS || 30);
const dateSpreadDays = Number(__ENV.DATE_SPREAD_DAYS || 180);
const cancelHolds = (__ENV.CANCEL_HOLDS || 'true').toLowerCase() === 'true';
const iterations = Number(__ENV.ITERATIONS || 0);
const targetRate = Number(__ENV.TARGET_RATE || 0);
const runSeed = Number(__ENV.RUN_SEED || 1);

const createLatency = new Trend('create_latency_ms', true);
const terminalLatency = new Trend('terminal_latency_ms', true);
const cancelLatency = new Trend('cancel_latency_ms', true);
const bookingsCreated = new Counter('bookings_created_total');
const bookingsTerminal = new Counter('bookings_terminal_total');
const bookingFailures = new Counter('booking_failures_total');
const terminalTimeouts = new Counter('terminal_timeouts_total');
const successfulIterations = new Rate('successful_iterations');

export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  scenarios: {
    create_and_poll: targetRate > 0
      ? {
          executor: 'constant-arrival-rate',
          exec: 'createAndPoll',
          rate: targetRate,
          timeUnit: __ENV.RATE_TIME_UNIT || '1s',
          duration: __ENV.DURATION || '60s',
          preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || Math.max(10, targetRate * 2)),
          maxVUs: Number(__ENV.MAX_VUS || Math.max(50, targetRate * 10)),
          gracefulStop: __ENV.GRACEFUL_STOP || '35s',
          tags: { app_thread_mode: mode },
        }
      : iterations > 0
      ? {
          executor: 'shared-iterations',
          exec: 'createAndPoll',
          vus: Number(__ENV.VUS || 1),
          iterations,
          maxDuration: __ENV.MAX_DURATION || '45s',
          tags: { app_thread_mode: mode },
        }
      : {
          executor: 'constant-vus',
          exec: 'createAndPoll',
          vus: Number(__ENV.VUS || 10),
          duration: __ENV.DURATION || '60s',
          gracefulStop: __ENV.GRACEFUL_STOP || '35s',
          tags: { app_thread_mode: mode },
        },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    create_latency_ms: [`p(95)<${__ENV.CREATE_P95_MS || 1000}`],
    terminal_latency_ms: [`p(95)<${__ENV.TERMINAL_P95_MS || 10000}`],
    successful_iterations: ['rate>0.95'],
    terminal_timeouts_total: ['count==0'],
  },
};

export function setup() {
  if (!token) {
    throw new Error('TOKEN is required. Pass a valid booking-service user JWT with -e TOKEN=...');
  }
  if (categoryIds.length === 0) {
    throw new Error('CATEGORY_IDS must contain at least one positive category id');
  }

  const health = http.get(`${baseUrl}/actuator/health`, {
    tags: { operation: 'health' },
    timeout: '5s',
  });
  if (!check(health, { 'booking-service is ready': (response) => response.status === 200 })) {
    throw new Error(`booking-service is not ready: ${health.status} ${health.body}`);
  }
}

export function createAndPoll() {
  const iterationStartedAt = Date.now();
  const categoryId = categoryIds[(__VU + __ITER) % categoryIds.length];
  const dateShift = dateOffsetDays + ((runSeed + __VU * 31 + __ITER) % dateSpreadDays);
  const checkInDate = dateFromToday(dateShift);
  const checkOutDate = dateFromToday(dateShift + 1);
  const idempotencyKey = `k6-${mode}-${Date.now()}-${__VU}-${__ITER}`;
  const params = {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      'Idempotency-Key': idempotencyKey,
    },
    tags: { operation: 'create_booking' },
    timeout: __ENV.HTTP_TIMEOUT || '10s',
  };
  const payload = JSON.stringify({
    categoryId,
    checkInDate,
    checkOutDate,
    guests: 2,
    adultCount: 2,
    childrenCount: 0,
    tariffCode: __ENV.TARIFF_CODE || null,
    promoCode: null,
  });

  const createResponse = http.post(`${baseUrl}/booking`, payload, params);
  createLatency.add(createResponse.timings.duration);

  const created = check(createResponse, {
    'booking created (201)': (response) => response.status === 201,
    'create response has id': (response) => response.status === 201 && Boolean(safeJson(response)?.id),
  });
  if (!created) {
    if (__ITER === 0) {
      console.error(`create booking failed: status=${createResponse.status}, body=${createResponse.body}`);
    }
    bookingFailures.add(1);
    successfulIterations.add(false);
    sleep(0.25);
    return;
  }

  bookingsCreated.add(1);
  const bookingId = safeJson(createResponse).id;
  let status = safeJson(createResponse).status;
  let terminal = status !== 'CREATED';
  const deadline = Date.now() + terminalTimeoutMs;

  while (!terminal && Date.now() < deadline) {
    sleep(pollIntervalMs / 1000);
    const pollResponse = http.get(`${baseUrl}/booking/${bookingId}`, {
      headers: { Authorization: `Bearer ${token}` },
      tags: { operation: 'poll_booking' },
      timeout: __ENV.HTTP_TIMEOUT || '10s',
    });
    if (!check(pollResponse, { 'poll returned 200': (response) => response.status === 200 })) {
      continue;
    }
    status = safeJson(pollResponse)?.status;
    terminal = status && status !== 'CREATED';
  }

  if (!terminal) {
    terminalTimeouts.add(1);
    successfulIterations.add(false);
    return;
  }

  terminalLatency.add(Date.now() - iterationStartedAt);
  bookingsTerminal.add(1, { status });
  const succeeded = status === 'HOLD';
  if (!succeeded) {
    bookingFailures.add(1, { status: status || 'unknown' });
  }

  if (cancelHolds && status === 'HOLD') {
    const cancelResponse = http.patch(`${baseUrl}/booking/${bookingId}/cancel`, null, {
      headers: { Authorization: `Bearer ${token}` },
      tags: { operation: 'cancel_booking' },
      timeout: __ENV.HTTP_TIMEOUT || '10s',
    });
    cancelLatency.add(cancelResponse.timings.duration);
    check(cancelResponse, { 'HOLD booking cancelled': (response) => response.status === 200 });
  }

  successfulIterations.add(succeeded);
}

export default createAndPoll;

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
