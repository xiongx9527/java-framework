SET @tiny_engine_sql = (
	SELECT IF(COUNT(*) = 0, 'ALTER TABLE t_tenant ADD COLUMN platform_domain varchar(255) NULL COMMENT ''平台域名''', 'SELECT 1')
	FROM information_schema.COLUMNS
	WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_tenant' AND COLUMN_NAME = 'platform_domain'
);
PREPARE tiny_engine_stmt FROM @tiny_engine_sql;
EXECUTE tiny_engine_stmt;
DEALLOCATE PREPARE tiny_engine_stmt;

SET @tiny_engine_sql = (
	SELECT IF(COUNT(*) = 0, 'ALTER TABLE t_tenant ADD COLUMN custom_domain varchar(255) NULL COMMENT ''自有域名''', 'SELECT 1')
	FROM information_schema.COLUMNS
	WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_tenant' AND COLUMN_NAME = 'custom_domain'
);
PREPARE tiny_engine_stmt FROM @tiny_engine_sql;
EXECUTE tiny_engine_stmt;
DEALLOCATE PREPARE tiny_engine_stmt;

SET @tiny_engine_sql = (
	SELECT IF(COUNT(*) = 0, 'CREATE UNIQUE INDEX u_idx_tenant_platform_domain ON t_tenant (platform_domain)', 'SELECT 1')
	FROM information_schema.STATISTICS
	WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_tenant' AND INDEX_NAME = 'u_idx_tenant_platform_domain'
);
PREPARE tiny_engine_stmt FROM @tiny_engine_sql;
EXECUTE tiny_engine_stmt;
DEALLOCATE PREPARE tiny_engine_stmt;

SET @tiny_engine_sql = (
	SELECT IF(COUNT(*) = 0, 'CREATE UNIQUE INDEX u_idx_tenant_custom_domain ON t_tenant (custom_domain)', 'SELECT 1')
	FROM information_schema.STATISTICS
	WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_tenant' AND INDEX_NAME = 'u_idx_tenant_custom_domain'
);
PREPARE tiny_engine_stmt FROM @tiny_engine_sql;
EXECUTE tiny_engine_stmt;
DEALLOCATE PREPARE tiny_engine_stmt;
