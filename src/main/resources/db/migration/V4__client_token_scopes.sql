ALTER TABLE client_api_tokens ADD COLUMN last_used_at TIMESTAMP;
ALTER TABLE client_api_tokens ADD COLUMN revoked_at TIMESTAMP;

CREATE TABLE client_api_token_scopes (
    client_api_token_id BIGINT NOT NULL REFERENCES client_api_tokens(id),
    scope VARCHAR(80) NOT NULL,
    PRIMARY KEY (client_api_token_id, scope)
);

INSERT INTO client_api_token_scopes (client_api_token_id, scope)
SELECT id, 'LICENSE_VALIDATE' FROM client_api_tokens;
INSERT INTO client_api_token_scopes (client_api_token_id, scope)
SELECT id, 'MACHINE_ACTIVATE' FROM client_api_tokens;
INSERT INTO client_api_token_scopes (client_api_token_id, scope)
SELECT id, 'MACHINE_HEARTBEAT' FROM client_api_tokens;
INSERT INTO client_api_token_scopes (client_api_token_id, scope)
SELECT id, 'MACHINE_DEACTIVATE' FROM client_api_tokens;
INSERT INTO client_api_token_scopes (client_api_token_id, scope)
SELECT id, 'OFFLINE_CHECKOUT' FROM client_api_tokens;
INSERT INTO client_api_token_scopes (client_api_token_id, scope)
SELECT id, 'OFFLINE_VERIFY' FROM client_api_tokens;
INSERT INTO client_api_token_scopes (client_api_token_id, scope)
SELECT id, 'OFFLINE_PUBLIC_KEY' FROM client_api_tokens;
