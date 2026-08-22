package com.xiongx9527.fw.plus.tinyengine.support;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinyEngineMysqlManifestCompatibilityTest {

	@Test
	void shouldUseMySql57CompatibleIdempotentRoleColumnDrop() throws Exception {
		String resource = "META-INF/tiny-engine/sql/mysql/07_update_table_ddl_2025_1014.sql";
		InputStream input = getClass().getClassLoader().getResourceAsStream(resource);
		assertNotNull(input, "Missing MySQL migration " + resource);
		String sql;
		try {
			sql = read(input);
		} finally {
			input.close();
		}

		assertFalse(sql.toUpperCase().contains("DROP COLUMN IF EXISTS"));
		assertTrue(sql.contains("information_schema.COLUMNS"));
		assertTrue(sql.contains("PREPARE tiny_engine_drop_user_role_stmt"));
		assertTrue(sql.contains("DEALLOCATE PREPARE tiny_engine_drop_user_role_stmt"));
	}

	private String read(InputStream input) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		byte[] buffer = new byte[512];
		int count;
		while ((count = input.read(buffer)) >= 0) {
			output.write(buffer, 0, count);
		}
		return new String(output.toByteArray(), StandardCharsets.UTF_8);
	}
}
