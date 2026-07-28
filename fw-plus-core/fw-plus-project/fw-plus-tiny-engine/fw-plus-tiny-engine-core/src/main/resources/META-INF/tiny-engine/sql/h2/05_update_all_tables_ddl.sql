ALTER TABLE t_component DROP CONSTRAINT u_idx_component;
CREATE INDEX u_idx_component ON t_component (tenant_id, name_en, version, library_id);

ALTER TABLE t_datasource DROP CONSTRAINT u_idx_datasource;
CREATE INDEX u_idx_datasource ON t_datasource (tenant_id, platform_id, name, app_id);

ALTER TABLE t_platform_history ALTER COLUMN sub_count INT NULL;
ALTER TABLE t_platform_history ALTER COLUMN publish_url VARCHAR(255) NULL;

ALTER TABLE t_app ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_app_extension ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_block ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_block_carriers_relation ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_block_group ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_block_history ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_business_category ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_component ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_component_library ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_datasource ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_i18n_entry ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_material ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_material_history ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_page ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_page_history ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_page_template ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_platform ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_platform_history ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_task_record ALTER COLUMN tenant_id VARCHAR(60) NULL;
ALTER TABLE t_user ALTER COLUMN tenant_id VARCHAR(60) NULL;
