-- H2 示例需要默认平台，应用创建请求未携带 platformId 时由后端自动选择该平台。
INSERT INTO `t_platform` (`id`, `name`, `published`, `description`, `latest_version`, `latest_history_id`, `material_history_id`, `is_default`, `tenant_id`, `site_id`, `created_by`, `last_updated_by`, `created_time`, `last_updated_time`)
VALUES (1, 'default', 1, 'TinyEngine H2 示例平台', '1.0.0', 1, 1, 1, '1', '1', '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO `t_platform_history` (`id`, `ref_id`, `version`, `name`, `publish_url`, `description`, `material_history_id`, `sub_count`, `material_pkg_name`, `material_version`, `tenant_id`, `site_id`, `created_by`, `last_updated_by`, `created_time`, `last_updated_time`)
VALUES (1, 1, '1.0.0', 'default', 'http://tinyengine.com', 'TinyEngine H2 示例平台', 1, 1, '@opentiny/lowcode-alpha-material-materialstwo-1505', '1.0.8', '1', '1', '1', '1', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
