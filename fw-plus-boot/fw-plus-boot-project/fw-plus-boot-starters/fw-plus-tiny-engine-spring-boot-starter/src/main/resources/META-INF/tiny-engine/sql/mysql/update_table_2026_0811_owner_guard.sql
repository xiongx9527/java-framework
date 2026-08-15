SET @owner_guard_column_exists = (
	SELECT COUNT(*) FROM information_schema.COLUMNS
	WHERE TABLE_SCHEMA = DATABASE()
		AND TABLE_NAME = 'r_auth_users_units_roles'
		AND COLUMN_NAME = 'owner_guard'
);
SET @owner_guard_column_sql = IF(
	@owner_guard_column_exists = 0,
	'ALTER TABLE r_auth_users_units_roles ADD COLUMN owner_guard TINYINT NULL COMMENT ''应用Owner唯一标记''',
	'SELECT 1'
);
PREPARE owner_guard_column_stmt FROM @owner_guard_column_sql;
EXECUTE owner_guard_column_stmt;
DEALLOCATE PREPARE owner_guard_column_stmt;

DELETE DUPLICATE_OWNER
FROM r_auth_users_units_roles DUPLICATE_OWNER
INNER JOIN t_permission_role DUPLICATE_ROLE
	ON DUPLICATE_ROLE.id = DUPLICATE_OWNER.role_id
	AND DUPLICATE_ROLE.role_type = 'app' AND DUPLICATE_ROLE.code = 'app_owner'
INNER JOIN r_auth_users_units_roles RETAINED_OWNER
	ON RETAINED_OWNER.tenant_id = DUPLICATE_OWNER.tenant_id
	AND RETAINED_OWNER.unit_id = DUPLICATE_OWNER.unit_id
	AND RETAINED_OWNER.unit_type = DUPLICATE_OWNER.unit_type
	AND RETAINED_OWNER.id < DUPLICATE_OWNER.id
INNER JOIN t_permission_role RETAINED_ROLE
	ON RETAINED_ROLE.id = RETAINED_OWNER.role_id
	AND RETAINED_ROLE.role_type = 'app' AND RETAINED_ROLE.code = 'app_owner'
WHERE DUPLICATE_OWNER.unit_type = 'app';

UPDATE r_auth_users_units_roles UR
SET owner_guard = CASE WHEN EXISTS (
	SELECT 1 FROM t_permission_role PR
	WHERE PR.id = UR.role_id AND PR.role_type = 'app' AND PR.code = 'app_owner'
) AND UR.unit_type = 'app' THEN 1 ELSE NULL END;

SET @owner_guard_index_exists = (
	SELECT COUNT(*) FROM information_schema.STATISTICS
	WHERE TABLE_SCHEMA = DATABASE()
		AND TABLE_NAME = 'r_auth_users_units_roles'
		AND INDEX_NAME = 'u_idx_app_owner_guard'
);
SET @owner_guard_index_sql = IF(
	@owner_guard_index_exists = 0,
	'CREATE UNIQUE INDEX u_idx_app_owner_guard ON r_auth_users_units_roles (tenant_id, unit_id, unit_type, owner_guard)',
	'SELECT 1'
);
PREPARE owner_guard_index_stmt FROM @owner_guard_index_sql;
EXECUTE owner_guard_index_stmt;
DEALLOCATE PREPARE owner_guard_index_stmt;
