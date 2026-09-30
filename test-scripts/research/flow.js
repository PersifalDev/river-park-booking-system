import http from 'k6/http';
import execution from 'k6/execution';
import { sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { bookingPayload, hasPaymentNotification } from './workload.js';

const bookingUrl = __ENV.BOOKING_BASE_URL || __ENV.BASE_URL || 'http://localhost:18084';
const paymentUrl = __ENV.PAYMENT_BASE_URL || 'http://localhost:18087';
const notificationUrl = __ENV.NOTIFICATION_BASE_URL || 'http://localhost:18088';
const tokens = (__ENV.TOKENS || __ENV.TOKEN || '').split(',').filter(Boolean);
const ids = (__ENV.CATEGORY_IDS || '1,2,3').split(',').map(Number);
const mode = __ENV.WORK_MODE || 'ASYNC';
const threadType = __ENV.THREAD_TYPE || __ENV.MODE || 'platform';
const runId = __ENV.RUN_ID || 'manual';
const seed = Number(__ENV.RUN_SEED || 1);
const baseDate = __ENV.BASE_DATE || new Date(Date.now() + 30 * 86400000).toISOString().slice(0, 10);
const timeoutMs = Number(__ENV.COMPLETION_TIMEOUT_MS || 45000);
const httpTimeoutMs = Number(__ENV.HTTP_TIMEOUT_MS || 15000);
const intervalMs = Number(__ENV.POLL_INTERVAL_MS || 250);
const rate = Number(__ENV.TARGET_RATE || 0);
const iterations = Number(__ENV.ITERATIONS || 0);
const tags = { work_mode: mode, thread_type: threadType };

const responseLatency = new Trend('booking_response_latency_ms', true);
const holdLatency = new Trend('booking_hold_latency_ms', true);
const paymentLatency = new Trend('payment_completion_latency_ms', true);
const notificationLatency = new Trend('notification_completion_latency_ms', true);
const completionLatency = new Trend('business_completion_latency_ms', true);
const responseSamples = new Counter('booking_response_samples');
const holdSamples = new Counter('booking_hold_samples');
const paymentSamples = new Counter('payment_completion_samples');
const notificationSamples = new Counter('notification_completion_samples');
const completionSamples = new Counter('business_completion_samples');
const accepted = new Counter('accepted_bookings');
const completed = new Counter('completed_bookings');
const acceptanceRate = new Rate('booking_acceptance_rate');
const completionRate = new Rate('accepted_completion_rate');
const successRate = new Rate('successful_iterations');
const responseAlreadyHold = new Rate('response_already_hold');
const timeouts = new Counter('completion_timeouts_total');
const failures = new Counter('booking_failures_total');
const cleanupFailures = new Counter('cleanup_failures_total');

const scenario = {
  exec: 'createBooking', tags,
  gracefulStop: `${Math.ceil((timeoutMs + 2 * httpTimeoutMs) / 1000)}s`,
};
export const options = {
  summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
  scenarios: { research: rate > 0 ? {
    ...scenario, executor: 'constant-arrival-rate', rate, timeUnit: '1s',
    duration: __ENV.DURATION || '60s', preAllocatedVUs: Number(__ENV.PRE_ALLOCATED_VUS || 30),
    maxVUs: Number(__ENV.MAX_VUS || 200),
  } : iterations > 0 ? {
    ...scenario, executor: 'shared-iterations', vus: Number(__ENV.VUS || 1), iterations,
    maxDuration: __ENV.MAX_DURATION || '10m',
  } : {
    ...scenario, executor: 'constant-vus', vus: Number(__ENV.VUS || 10), duration: __ENV.DURATION || '60s',
  } },
  thresholds: {
    'http_req_failed{operation:create_booking}': ['rate<0.01'],
    booking_acceptance_rate: ['rate>0.95'], accepted_completion_rate: ['rate>0.95'],
    successful_iterations: ['rate>0.95'], completion_timeouts_total: ['count==0'],
    booking_response_latency_ms: [`p(95)<${__ENV.RESPONSE_P95_MS || 10000}`],
    business_completion_latency_ms: [`p(95)<${__ENV.COMPLETION_P95_MS || timeoutMs}`],
  },
};

export function setup() {
  for (const counter of [responseSamples, holdSamples, paymentSamples, notificationSamples,
    completionSamples, accepted, completed, timeouts, failures, cleanupFailures]) counter.add(0);
  if (!tokens.length || ids.some((id) => !Number.isInteger(id) || id < 1)) {
    throw new Error('TOKEN(S) and positive CATEGORY_IDS are required');
  }
  if (timeoutMs <= 0 || httpTimeoutMs <= 0 || intervalMs <= 0) throw new Error('Timeouts must be positive');
  bookingPayload(0, seed, baseDate, ids);
  for (const url of [bookingUrl, paymentUrl, notificationUrl]) {
    if (http.get(`${url}/actuator/health`, { timeout: '5s', tags: { operation: 'health' } }).status !== 200) {
      throw new Error(`Service unavailable: ${url}`);
    }
  }
}

export function createBooking() {
  const iteration = execution.scenario.iterationInTest;
  const auth = { Authorization: `Bearer ${tokens[iteration % tokens.length]}` };
  const key = `research-${runId}-${iteration}`;
  const payload = bookingPayload(iteration, seed, baseDate, ids);
  payload.tariffCode = __ENV.TARIFF_CODE || null;
  const started = Date.now();
  const deadline = started + timeoutMs;
  const outcome = { runId, iteration, key, bookingId: null, accepted: false, completed: false,
    startedAt: new Date(started).toISOString(), responseStatus: null,
    holdMs: null, paymentMs: null, notificationMs: null, completionMs: null, cleanup: 'unknown' };
  console.log(`RESEARCH_ATTEMPT ${JSON.stringify({ runId, iteration, key, payload })}`);
  try {
    const response = http.post(`${bookingUrl}/booking`, JSON.stringify(payload), {
      headers: { ...auth, 'Content-Type': 'application/json', 'Idempotency-Key': key },
      timeout: `${Math.min(httpTimeoutMs, timeoutMs)}ms`, tags: { ...tags, operation: 'create_booking' },
    });
    responseLatency.add(response.timings.duration, tags);
    responseSamples.add(1);
    const body = json(response);
    outcome.responseStatus = response.status;
    outcome.bookingId = body && body.id;
    outcome.accepted = response.status === 201 && Boolean(outcome.bookingId);
    if (!outcome.accepted) {
      outcome.error = `create:${response.status}`;
      failures.add(1);
      return;
    }
    accepted.add(1);
    responseAlreadyHold.add(body.status === 'HOLD');
    if (body.status === 'HOLD') outcome.holdMs = Date.now() - started;
    let status = body.status;
    while (Date.now() < deadline) {
      const requestParams = (operation, expected) => ({ headers: auth,
        timeout: `${Math.max(1, Math.min(httpTimeoutMs, deadline - Date.now()))}ms`,
        tags: { ...tags, operation }, responseCallback: http.expectedStatuses(...expected) });
      const responses = http.batch([
        { method: 'GET', url: `${bookingUrl}/booking/${outcome.bookingId}`, params: requestParams('poll_booking', [200]) },
        { method: 'GET', url: `${paymentUrl}/payments/booking/${outcome.bookingId}`, params: requestParams('poll_payment', [200, 404]) },
        { method: 'GET', url: `${notificationUrl}/notifications?bookingId=${outcome.bookingId}&pageNumber=0&pageSize=100`,
          params: requestParams('poll_notification', [200]) },
      ]);
      const observedMs = Date.now() - started;
      const booking = json(responses[0]);
      if (responses[0].status === 200 && booking) status = booking.status;
      if (status === 'HOLD' && outcome.holdMs === null) outcome.holdMs = observedMs;
      if (['FAILED', 'CANCELLED', 'EXPIRED'].includes(status)) {
        outcome.error = `booking:${status}`; failures.add(1); break;
      }
      const payment = json(responses[1]);
      if (responses[1].status === 200 && payment && payment.status === 'PENDING' && outcome.paymentMs === null) {
        outcome.paymentMs = observedMs;
      }
      if (responses[2].status === 200 && hasPaymentNotification(json(responses[2]), outcome.bookingId)
          && outcome.notificationMs === null) outcome.notificationMs = observedMs;
      if (outcome.holdMs !== null && outcome.paymentMs !== null && outcome.notificationMs !== null
          && Date.now() <= deadline) {
        outcome.completed = true;
        outcome.completionMs = Math.max(outcome.holdMs, outcome.paymentMs, outcome.notificationMs);
        completed.add(1); break;
      }
      if (Date.now() < deadline) sleep(Math.min(intervalMs, deadline - Date.now()) / 1000);
    }
    if (!outcome.completed && !outcome.error) { outcome.error = 'completion_timeout'; timeouts.add(1); }
  } catch (error) {
    outcome.error = String(error); failures.add(1);
  } finally {
    acceptanceRate.add(outcome.accepted);
    for (const [value, trend, samples] of [
      [outcome.holdMs, holdLatency, holdSamples], [outcome.paymentMs, paymentLatency, paymentSamples],
      [outcome.notificationMs, notificationLatency, notificationSamples],
      [outcome.completionMs, completionLatency, completionSamples],
    ]) {
      if (value !== null) { trend.add(value, tags); samples.add(1); }
    }
    if (outcome.accepted) completionRate.add(outcome.completed);
    successRate.add(outcome.completed);
    if (outcome.bookingId) {
      try {
        const response = http.patch(`${bookingUrl}/booking/${outcome.bookingId}/cancel`, null, {
          headers: auth, timeout: `${httpTimeoutMs}ms`, tags: { ...tags, operation: 'cleanup' },
          responseCallback: http.expectedStatuses(200, 409),
        });
        outcome.cleanup = response.status === 200 ? 'cancelled' : `status:${response.status}`;
        if (response.status !== 200) cleanupFailures.add(1);
      } catch (error) {
        outcome.cleanup = `error:${String(error)}`;
        cleanupFailures.add(1);
      }
    }
    outcome.finishedAt = new Date().toISOString();
    console.log(`RESEARCH_OUTCOME ${JSON.stringify(outcome)}`);
  }
}

export default createBooking;
export function handleSummary(data) {
  const path = __ENV.SUMMARY_PATH || 'summary.json';
  return { [path]: JSON.stringify(data, null, 2), stdout: `Research summary: ${path}\n` };
}
function json(response) { try { return response.json(); } catch (_) { return null; } }
