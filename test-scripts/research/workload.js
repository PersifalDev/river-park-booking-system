export function bookingPayload(iteration, seed, baseDate, categoryIds) {
  if (!Number.isInteger(iteration) || iteration < 0 || !Number.isInteger(seed) || categoryIds.length === 0) {
    throw new Error('Invalid workload parameters');
  }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(baseDate)) throw new Error('BASE_DATE must be YYYY-MM-DD');
  const base = Date.parse(`${baseDate}T00:00:00Z`);
  if (!Number.isFinite(base) || new Date(base).toISOString().slice(0, 10) !== baseDate) {
    throw new Error('Invalid BASE_DATE');
  }
  if (categoryIds.some((id) => !Number.isInteger(id) || id < 1)) throw new Error('Invalid category IDs');
  const slot = ((iteration + seed * 17) % 180 + 180) % 180;
  const date = (offset) => new Date(base + offset * 86400000).toISOString().slice(0, 10);
  return {
    categoryId: categoryIds[((iteration + seed) % categoryIds.length + categoryIds.length) % categoryIds.length],
    checkInDate: date(slot), checkOutDate: date(slot + 1),
    guests: 2, adultCount: 2, childrenCount: 0, tariffCode: null, promoCode: null,
  };
}

export function hasPaymentNotification(body, bookingId) {
  return (body && Array.isArray(body.content) ? body.content : []).some((notification) =>
    notification.bookingId === bookingId && notification.type === 'PAYMENT_PENDING');
}
