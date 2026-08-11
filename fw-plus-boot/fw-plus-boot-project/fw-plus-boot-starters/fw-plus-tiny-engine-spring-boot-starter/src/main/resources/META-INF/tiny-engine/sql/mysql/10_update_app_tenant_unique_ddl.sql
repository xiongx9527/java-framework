SET @tiny_engine_sql = (
	SELECT IF(COUNT(*) > 0, 'ALTER TABLE t_app DROP INDEX u_idx_app', 'SELECT 1')
	FROM information_schema.STATISTICS
	WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_app' AND INDEX_NAME = 'u_idx_app'
);
PREPARE tiny_engine_stmt FROM @tiny_engine_sql;
EXECUTE tiny_engine_stmt;
DEALLOCATE PREPARE tiny_engine_stmt;

SET @tiny_engine_sql = (
	SELECT IF(COUNT(*) > 0, 'ALTER TABLE t_app DROP INDEX u_idx_app_code', 'SELECT 1')
	FROM information_schema.STATISTICS
	WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_app' AND INDEX_NAME = 'u_idx_app_code'
);
PREPARE tiny_engine_stmt FROM @tiny_engine_sql;
EXECUTE tiny_engine_stmt;
DEALLOCATE PREPARE tiny_engine_stmt;

SET @tiny_engine_sql = (
	SELECT IF(COUNT(*) = 0, 'CREATE UNIQUE INDEX u_idx_app_name_tenant ON t_app (tenant_id, name)', 'SELECT 1')
	FROM information_schema.STATISTICS
	WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_app' AND INDEX_NAME = 'u_idx_app_name_tenant'
);
PREPARE tiny_engine_stmt FROM @tiny_engine_sql;
EXECUTE tiny_engine_stmt;
DEALLOCATE PREPARE tiny_engine_stmt;

SET @tiny_engine_sql = (
	SELECT IF(COUNT(*) = 0, 'CREATE UNIQUE INDEX u_idx_app_code_tenant ON t_app (tenant_id, code)', 'SELECT 1')
	FROM information_schema.STATISTICS
	WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_app' AND INDEX_NAME = 'u_idx_app_code_tenant'
);
PREPARE tiny_engine_stmt FROM @tiny_engine_sql;
EXECUTE tiny_engine_stmt;
DEALLOCATE PREPARE tiny_engine_stmt;
