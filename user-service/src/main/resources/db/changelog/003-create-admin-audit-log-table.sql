CREATE TABLE admin_audit_log
(
    id             UUID PRIMARY KEY,
    actor_user_id  BIGINT,
    actor_login    VARCHAR(255),
    actor_role     VARCHAR(64),
    action         VARCHAR(128)             NOT NULL,
    target_type    VARCHAR(64),
    target_id      VARCHAR(128),
    outcome        VARCHAR(32)              NOT NULL,
    request_id     VARCHAR(128),
    ip_address     VARCHAR(64),
    user_agent     VARCHAR(256),
    details        VARCHAR(1024),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_admin_audit_log_actor_user_id ON admin_audit_log (actor_user_id);
CREATE INDEX idx_admin_audit_log_created_at ON admin_audit_log (created_at);
CREATE INDEX idx_admin_audit_log_action ON admin_audit_log (action);
