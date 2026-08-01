ALTER TABLE t_tenant DROP CONSTRAINT u_idx_tenant;
CREATE UNIQUE INDEX u_idx_tenant ON t_tenant (name_en);

ALTER TABLE t_tenant ALTER COLUMN name_en VARCHAR(255) NOT NULL;
ALTER TABLE t_tenant ALTER COLUMN name_cn VARCHAR(255) NULL;
