import assert from 'node:assert/strict';
import { bookingPayload, hasPaymentNotification } from './workload.js';

for (let index = 0; index < 1000; index++) {
  assert.deepEqual(bookingPayload(index, 3, '2026-10-01', [1, 2, 3]),
    bookingPayload(index, 3, '2026-10-01', [1, 2, 3]));
  const value = bookingPayload(index, 3, '2026-10-01', [1, 2, 3]);
  assert.equal(Date.parse(value.checkOutDate) - Date.parse(value.checkInDate), 86400000);
}
assert.notDeepEqual(bookingPayload(0, 1, '2026-10-01', [1, 2]), bookingPayload(1, 1, '2026-10-01', [1, 2]));
assert.equal(hasPaymentNotification({content: [{bookingId:'a', type:'BOOKING_HOLD_CREATED'}]}, 'a'), false);
assert.equal(hasPaymentNotification({content: [{bookingId:'b', type:'PAYMENT_PENDING'}]}, 'a'), false);
assert.equal(hasPaymentNotification({content: [{bookingId:'a', type:'PAYMENT_PENDING'}]}, 'a'), true);
assert.throws(() => bookingPayload(0, 1, 'invalid', [1]));
assert.throws(() => bookingPayload(0, 1, '2026-02-31', [1]));
assert.throws(() => bookingPayload(0, 1, '2026-10-01', [0]));
console.log('Deterministic workload and notification predicate: passed');
