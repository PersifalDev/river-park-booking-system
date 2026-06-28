# Personal data and security policy

River Park services process user identifiers, logins, Telegram chat identifiers, booking data, payment intent data and notifications. Public UI must show the personal data processing consent and privacy policy before registration or booking flows that create a user profile.

## Technical controls

- Registration requires `personalDataConsentAccepted=true` and `privacyPolicyAccepted=true`.
- Accepted consent versions are stored in `user_consents`.
- Access tokens are short lived JWTs. Refresh tokens are opaque random tokens stored only as SHA-256 hashes.
- Refresh token rotation is enforced. Reusing a revoked refresh token revokes the whole token family.
- Internal service endpoints under `/api/v1/internal/**` require `X-Internal-Service-Token` when `INTERNAL_SERVICE_TOKEN` is configured.
- Admin mutations are written to `admin_audit_log` with actor, action, target, outcome, request id, IP and user agent.
- Structured JSON logs use `PiiMaskingStructuredLoggingJsonMembersCustomizer` to mask passwords, secrets, tokens, authorization values, emails and phone numbers.

## Retention defaults

| Data class | Default retention | Config |
| --- | ---: | --- |
| Refresh token history | 90 days | `USER_REFRESH_TOKEN_HISTORY_RETENTION` |
| Admin audit log | 365 days | `USER_ADMIN_AUDIT_LOG_RETENTION` |
| Inactive user data | 1095 days | `USER_INACTIVE_DATA_RETENTION` |
| Inactive bookings | 1095 days | `BOOKING_INACTIVE_DATA_RETENTION` |
| Booking outbox events | 90 days | `BOOKING_OUTBOX_RETENTION` |
| Payment records | 1825 days | `PAYMENT_RECORD_RETENTION` |
| Read notifications | 365 days | `NOTIFICATION_READ_RETENTION` |
| Unread notifications | 1095 days | `NOTIFICATION_UNREAD_RETENTION` |
| Telegram chat session cache | 90 days | `TELEGRAM_CHAT_SESSION_RETENTION` |

Retention values are exposed as application config now. Scheduled purge jobs should use these values before production launch.

## Required production env

- `JWT_SECRET_KEY`: strong HMAC key, at least 32 bytes.
- `JWT_LIFETIME`: access token lifetime in milliseconds.
- `JWT_REFRESH_LIFETIME`: refresh token lifetime in milliseconds.
- `INTERNAL_SERVICE_TOKEN`: high entropy shared token for internal API calls.
- `PERSONAL_DATA_POLICY_VERSION`: version shown to users.
- `PRIVACY_POLICY_VERSION`: version shown to users.
