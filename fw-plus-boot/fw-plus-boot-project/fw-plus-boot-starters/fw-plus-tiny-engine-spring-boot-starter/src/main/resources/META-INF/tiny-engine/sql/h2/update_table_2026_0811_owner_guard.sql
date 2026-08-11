ALTER TABLE r_auth_users_units_roles ADD COLUMN IF NOT EXISTS owner_guard TINYINT NULL;

DELETE FROM r_auth_users_units_roles
WHERE id IN (
	SELECT id FROM (
		SELECT UR.id,
			ROW_NUMBER() OVER (PARTITION BY UR.tenant_id, UR.unit_id, UR.unit_type ORDER BY UR.id) AS owner_rank
		FROM r_auth_users_units_roles UR
		INNER JOIN t_permission_role PR ON PR.id = UR.role_id
		WHERE UR.unit_type = 'app' AND PR.role_type = 'app' AND PR.code = 'app_owner'
	) DUPLICATE_OWNERS
	WHERE DUPLICATE_OWNERS.owner_rank > 1
);

UPDATE r_auth_users_units_roles UR
SET owner_guard = CASE WHEN EXISTS (
	SELECT 1 FROM t_permission_role PR
	WHERE PR.id = UR.role_id AND PR.role_type = 'app' AND PR.code = 'app_owner'
) AND UR.unit_type = 'app' THEN 1 ELSE NULL END;

CREATE UNIQUE INDEX IF NOT EXISTS u_idx_app_owner_guard
	ON r_auth_users_units_roles (tenant_id, unit_id, unit_type, owner_guard);
