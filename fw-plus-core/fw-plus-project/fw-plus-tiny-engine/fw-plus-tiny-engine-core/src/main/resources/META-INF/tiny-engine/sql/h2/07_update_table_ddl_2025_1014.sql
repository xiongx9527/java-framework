ALTER TABLE t_app ADD COLUMN is_template VARCHAR(100);
ALTER TABLE t_app ADD COLUMN industry_id INT;
ALTER TABLE t_app ADD COLUMN scene_id INT;

ALTER TABLE t_app DROP CONSTRAINT u_idx_app;
CREATE INDEX u_idx_app ON t_app (tenant_id, platform_id, name, is_template);

ALTER TABLE t_business_category ADD COLUMN business_group VARCHAR(100);

DROP INDEX u_idx_datasource;
CREATE INDEX u_idx_datasource ON t_datasource (tenant_id, platform_id, name, app_id);

ALTER TABLE t_app_extension ADD COLUMN platform_id INT;
ALTER TABLE t_app_extension DROP CONSTRAINT u_idx_app_extension;
CREATE INDEX u_idx_app_extension ON t_app_extension (tenant_id, platform_id, name, app_id);

ALTER TABLE t_model ADD COLUMN app_id INT;
ALTER TABLE t_model ADD COLUMN platform_id INT;
ALTER TABLE t_model DROP CONSTRAINT u_idx_model;
CREATE INDEX u_idx_model ON t_model (tenant_id, platform_id, app_id, name_cn, version);

ALTER TABLE t_user DROP COLUMN email;
ALTER TABLE t_user DROP COLUMN tenant_id;
ALTER TABLE t_user DROP COLUMN site_id;
ALTER TABLE t_user DROP COLUMN renter_id;
ALTER TABLE t_user DROP COLUMN created_by;
ALTER TABLE t_user DROP COLUMN last_updated_by;

ALTER TABLE t_user ADD COLUMN password VARCHAR(200);
ALTER TABLE t_user ADD COLUMN email VARCHAR(200);
ALTER TABLE t_user ADD COLUMN salt VARCHAR(200);
ALTER TABLE t_user ADD COLUMN public_key VARCHAR(200);
ALTER TABLE t_user ADD COLUMN private_key VARCHAR(200);
