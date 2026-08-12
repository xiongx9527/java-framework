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
    created_time TIMESTAMP NULL,
    last_updated_by VARCHAR(64),
    last_updated_time TIMESTAMP NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_model_service_tenant_app_key (tenant_id, app_id, service_key)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS t_ai_model (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL,
    app_id INT NOT NULL,
    service_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    label VARCHAR(128) NOT NULL,
    capabilities VARCHAR(2048),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_by VARCHAR(64),
    created_time TIMESTAMP NULL,
    last_updated_by VARCHAR(64),
    last_updated_time TIMESTAMP NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ai_model_tenant_app_service_name (tenant_id, app_id, service_id, name),
    KEY idx_ai_model_tenant_app (tenant_id, app_id),
    KEY idx_ai_model_service_id (service_id)
) ENGINE=InnoDB;
