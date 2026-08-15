ALTER TABLE r_auth_users_units_roles
	MODIFY COLUMN expired_time TIMESTAMP NULL DEFAULT NULL COMMENT '过期时间';

UPDATE t_permission_role SET code = 'app_admin', role_type = 'app'
WHERE name = 'Tinybuilder_App_Admin' AND code <> 'app_admin';
UPDATE t_permission_role SET code = 'app_dev', role_type = 'app'
WHERE name = 'Tinybuilder_App_Developer' AND code <> 'app_dev';

INSERT INTO t_permission_role
	(name, code, role_type, description, created_by, created_time, last_updated_by, last_updated_time)
SELECT 'Tinybuilder_App_Owner', 'app_owner', 'app', '应用创建者', 'system', CURRENT_TIMESTAMP, 'system', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM t_permission_role WHERE code = 'app_owner');

INSERT INTO r_auth_users_units_roles
	(user_id, unit_id, unit_type, tenant_id, role_id, expired_time,
	 created_by, created_time, last_updated_by, last_updated_time)
SELECT A.created_by, A.id, 'app', A.tenant_id, PR.id, NULL,
	A.created_by, CURRENT_TIMESTAMP, A.created_by, CURRENT_TIMESTAMP
FROM t_app A
INNER JOIN t_user U ON U.id = A.created_by AND U.tenant_id = A.tenant_id
INNER JOIN t_permission_role PR ON PR.code = 'app_owner' AND PR.role_type = 'app'
WHERE A.created_by IS NOT NULL
	AND NOT EXISTS (
		SELECT 1 FROM r_auth_users_units_roles UR
		WHERE UR.user_id = A.created_by AND UR.unit_id = A.id AND UR.unit_type = 'app'
	)
	AND NOT EXISTS (
		SELECT 1 FROM r_auth_users_units_roles OWNER_UR
		INNER JOIN t_permission_role OWNER_PR ON OWNER_PR.id = OWNER_UR.role_id
		WHERE OWNER_UR.tenant_id = A.tenant_id AND OWNER_UR.unit_id = A.id
			AND OWNER_UR.unit_type = 'app' AND OWNER_PR.role_type = 'app' AND OWNER_PR.code = 'app_owner'
	);
