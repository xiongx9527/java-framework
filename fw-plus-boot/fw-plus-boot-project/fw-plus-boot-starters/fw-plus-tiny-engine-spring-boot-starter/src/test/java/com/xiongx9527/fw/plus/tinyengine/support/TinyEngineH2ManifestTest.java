package com.xiongx9527.fw.plus.tinyengine.support;

import com.xiongx9527.fw.plus.tinyengine.support.sql.TinyEngineSqlScriptManifest;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinyEngineH2ManifestTest {

	@Test
	void shouldExecuteH2ManifestScriptsInOrder() throws Exception {
		List<String> expectedScripts = Arrays.asList(
				"classpath:META-INF/tiny-engine/sql/h2/01_create_all_tables_ddl_v1.0.0.h2.sql",
				"classpath:META-INF/tiny-engine/sql/h2/02_create_model_ddl_2025_0717.sql",
				"classpath:META-INF/tiny-engine/sql/h2/03_create_resources_ddl_2025_0902.sql",
				"classpath:META-INF/tiny-engine/sql/h2/04_create_permission_table_ddl_2025_1029.sql",
				"classpath:META-INF/tiny-engine/sql/h2/05_update_all_tables_ddl.sql",
				"classpath:META-INF/tiny-engine/sql/h2/06_update_tables_ddl_v1.0.0_2025_0527.sql",
				"classpath:META-INF/tiny-engine/sql/h2/07_update_table_ddl_2025_1014.sql",
				"classpath:META-INF/tiny-engine/sql/h2/08_update_table_ddl_2025_1217.sql",
				"classpath:META-INF/tiny-engine/sql/h2/09_update_table_ddl_2025_1229.sql",
				"classpath:META-INF/tiny-engine/sql/h2/00_init_default_platform.h2.sql",
				"classpath:META-INF/tiny-engine/sql/h2/init_data_2025_1023.sql",
				"classpath:META-INF/tiny-engine/sql/h2/init_data_2025_1125.sql",
				"classpath:META-INF/tiny-engine/sql/h2/init_data_2026_0810_app_roles.sql",
				"classpath:META-INF/tiny-engine/sql/h2/update_table_2026_0811_owner_guard.sql",
				"classpath:META-INF/tiny-engine/sql/h2/14_create_ai_model_config_ddl.sql");
		List<String> scripts = new TinyEngineSqlScriptManifest().getScripts("h2");
		assertEquals(expectedScripts, scripts);

		List<String> executedScripts = new ArrayList<String>();
		try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:tiny_engine_manifest;DB_CLOSE_DELAY=-1")) {
			for (String script : scripts) {
				executeScript(connection, script);
				executedScripts.add(script);
			}
			try (Statement statement = connection.createStatement();
				 ResultSet resultSet = statement.executeQuery(
						"SELECT COUNT(*) FROM t_platform WHERE tenant_id = '1' AND is_default = TRUE")) {
				assertTrue(resultSet.next());
				assertEquals(1, resultSet.getInt(1), "H2 示例必须提供默认平台");
			}
		}

		assertEquals(expectedScripts, executedScripts);
	}

	private void executeScript(Connection connection, String location) throws Exception {
		String resourcePath = location.substring("classpath:".length());
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
		assertNotNull(inputStream, "Missing manifest script " + location);
		try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
			RunScript.execute(connection, reader);
		}
	}
}
