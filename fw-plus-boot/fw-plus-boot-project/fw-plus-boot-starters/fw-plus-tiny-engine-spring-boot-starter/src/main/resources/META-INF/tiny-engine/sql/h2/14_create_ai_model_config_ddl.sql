CREATE TABLE IF NOT EXISTS t_ai_model_service (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL,
    app_id INT NOT NULL,
    service_key VARCHAR(64) NOT NULL,
    provider VARCHAR(64) NOT NULL,
    label VARCHAR(128) NOT NULL,
    base_url VARCHAR(1024) NOT NULL,
    api_key VARCHAR(2048),
    allow_empty_api_key BOOLEAN NOT NULL DEFAULT FALSE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    created_time TIMESTAMP,
    last_updated_by VARCHAR(64),
    last_updated_time TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (tenant_id, app_id, service_key)
);

CREATE TABLE IF NOT EXISTS t_ai_model (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL,
    app_id INT NOT NULL,
    service_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    label VARCHAR(128) NOT NULL,
    capabilities VARCHAR(2048),
    settings VARCHAR(4096),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    created_time TIMESTAMP,
    last_updated_by VARCHAR(64),
    last_updated_time TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (tenant_id, app_id, service_id, name)
);

CREATE INDEX IF NOT EXISTS idx_ai_model_tenant_app ON t_ai_model (tenant_id, app_id);
CREATE INDEX IF NOT EXISTS idx_ai_model_service_id ON t_ai_model (service_id);

ALTER TABLE t_ai_model ADD COLUMN IF NOT EXISTS settings VARCHAR(4096);
