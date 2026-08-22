ALTER TABLE t_block_group DROP CONSTRAINT u_idx_block_group;
CREATE INDEX u_idx_block_group ON t_block_group (tenant_id, platform_id, name, app_id);
