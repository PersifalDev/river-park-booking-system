import http from 'k6/http';
import crypto from 'k6/crypto';
import { check, sleep } from 'k6';
import { bookingPayload } from './workload.js';

const tokens = (__ENV.TOKENS || '').split(',').filter(Boolean);
const runId = __ENV.RUN_ID || 'correctness';
const baseDate = __ENV.BASE_DATE;
const category = Number((__ENV.CATEGORY_IDS || '1').split(',')[0]);
const bookingUrl = 'http://localhost:18084';
const paymentUrl = 'http://localhost:18087';
const notificationUrl = 'http://localhost:18088';
export const options = { vus: 1, iterations: 1, thresholds: { checks: ['rate==1'] } };

function json(response) { try { return response.json(); } catch (_) { return null; } }
function headers(index = 0) { return { Authorization: `Bearer ${tokens[index]}`, 'Content-Type': 'application/json' }; }
function post(key, payload) {
  return { method: 'POST', url: `${bookingUrl}/booking`, body: JSON.stringify(payload),
    params: { headers: { ...headers(), 'Idempotency-Key': key }, timeout: '20s',
      responseCallback: http.expectedStatuses(201, 400, 409) } };
}
function observe(id) {
  const responses = http.batch([
    ['GET', `${bookingUrl}/booking/${id}`, null, {headers: headers(), timeout:'5s'}],
    ['GET', `${paymentUrl}/payments/booking/${id}`, null, {headers: headers(), timeout:'5s', responseCallback:http.expectedStatuses(200,404)}],
    ['GET', `${notificationUrl}/notifications?bookingId=${id}&pageSize=100`, null, {headers:headers(), timeout:'5s'}],
  ]);
  return { booking: json(responses[0]), payment: json(responses[1]), notifications: json(responses[2]) };
}
function holdEventId(id) {
  const digest = crypto.md5(`booking-hold:${id}`, 'hex').split('');
  digest[12] = '3'; digest[16] = ((parseInt(digest[16], 16) & 3) | 8).toString(16);
  const value = digest.join('');
  return `${value.slice(0,8)}-${value.slice(8,12)}-${value.slice(12,16)}-${value.slice(16,20)}-${value.slice(20)}`;
}

export default function () {
  if (tokens.length < 2) throw new Error('At least two synthetic users are required');
  const knownIds = new Set();
  try {
    const payload = bookingPayload(0, 0, baseDate, [category]);
    const key = `research-${runId}-idempotency`;
    const responses = http.batch(Array.from({length: 8}, () => post(key, payload)));
    const ids = responses.map(json).map((value) => value && value.id).filter(Boolean);
    ids.forEach((id) => knownIds.add(id));
    const one = ids.length === 8 && new Set(ids).size === 1 && responses.every((response) => response.status === 201);
    check(one, {'8 parallel requests with one key return one booking': (value) => value});
    if (!one) throw new Error('Idempotency failed');
    const id = ids[0];
    let observed = null;
    const deadline = Date.now() + 60000;
    while (Date.now() < deadline) {
      observed = observe(id);
      const events = observed.notifications && observed.notifications.content || [];
      if (observed.booking && observed.booking.status === 'HOLD' && observed.payment && observed.payment.status === 'PENDING'
          && events.some((event) => event.type === 'PAYMENT_PENDING')
          && events.some((event) => event.type === 'BOOKING_HOLD_CREATED')) break;
      sleep(0.5);
    }
    check(observed, {'single booking reaches HOLD and PENDING': (value) => value && value.booking && value.booking.status === 'HOLD'
        && value.payment && value.payment.status === 'PENDING'});
    const stranger = http.get(`${notificationUrl}/notifications?bookingId=${id}&pageSize=100`, {headers:headers(1), timeout:'5s'});
    check(stranger, {'booking filter does not expose another user notifications': (response) => response.status === 200 && json(response).content.length === 0});

    if (!observed || !observed.booking || !observed.payment || !observed.notifications) {
      throw new Error('Business process did not become observable');
    }
    const booking = observed.booking;
    const event = { eventId: holdEventId(id), eventType:'BOOKING_HOLD_CREATED', source:'booking-service',
      correlationId:id, createdAt:new Date().toISOString(), payload:{ bookingId:id, bookingCode:booking.bookingCode,
        userId:booking.userId, roomCategoryId:booking.roomCategoryId, guests:booking.guests,
        adultCount:booking.adultCount, childrenCount:booking.childrenCount, checkInDate:booking.checkInDate,
        checkOutDate:booking.checkOutDate, priceAmount:booking.priceAmount, bookingStatus:booking.status,
        holdExpiresAt:booking.holdExpiresAt, cancellationReason:booking.cancellationReason || null } };
    const internal = { 'X-Internal-Service-Token': __ENV.INTERNAL_SERVICE_TOKEN, 'Content-Type':'application/json' };
    const replay = [];
    for (let index = 0; index < 4; index++) {
      replay.push(['POST', `${paymentUrl}/api/v1/internal/booking-events`, JSON.stringify(event), {headers:internal, timeout:'10s'}]);
      replay.push(['POST', `${notificationUrl}/api/v1/internal/events/booking`, JSON.stringify(event), {headers:internal, timeout:'10s'}]);
    }
    const replayed = http.batch(replay);
    check(replayed, {'duplicate deliveries are accepted': (values) => values.every((value) => value.status === 204)});
    const after = observe(id);
    check(after, {
      'duplicate HOLD does not create another payment': (value) => value.payment && observed.payment && value.payment.id === observed.payment.id,
      'duplicate HOLD does not duplicate notifications': (value) => value.notifications.content.filter((n) => n.type === 'BOOKING_HOLD_CREATED').length === 1,
      'PAYMENT_PENDING notification occurs once': (value) => value.notifications.content.filter((n) => n.type === 'PAYMENT_PENDING').length === 1,
    });

    const conflictDate = new Date(Date.parse(`${baseDate}T00:00:00Z`) + 86400000).toISOString().slice(0,10);
    const lastRoom = bookingPayload(0, 0, conflictDate, [category]);
    const race = http.batch(Array.from({length: 8}, (_, index) => post(`research-${runId}-last-room-${index}`, lastRoom)));
    const raceIds = race.map(json).map((value) => value && value.id).filter(Boolean);
    raceIds.forEach((value) => knownIds.add(value));
    let states = [];
    const raceDeadline = Date.now() + 60000;
    while (Date.now() < raceDeadline) {
      states = raceIds.map((value) => json(http.get(`${bookingUrl}/booking/${value}`, {headers:headers(), timeout:'5s'})));
      if (states.every((value) => value && value.status !== 'CREATED')) break;
      sleep(0.5);
    }
    check(states, {
      'last available unit yields exactly one HOLD': (values) => values.filter((value) => value && value.status === 'HOLD').length === 1,
      'other accepted contenders finish with FAILED': (values) => values.every((value) => value && ['HOLD','FAILED'].includes(value.status)),
      'capacity contention does not return server errors': () => race.every((response) => [201,400,409].includes(response.status)),
    });
  } finally {
    for (const id of knownIds) {
      const booking = json(http.get(`${bookingUrl}/booking/${id}`, {headers:headers(), timeout:'5s'}));
      if (booking && ['CREATED','HOLD','CONFIRMED'].includes(booking.status)) {
        const response = http.patch(`${bookingUrl}/booking/${id}/cancel`, null, {headers:headers(), timeout:'15s'});
        check(response, {'known active correctness booking cancelled': (value) => value.status === 200});
      }
    }
    console.log(`CORRECTNESS_REGISTRY ${JSON.stringify({runId, bookingIds:Array.from(knownIds)})}`);
  }
}
