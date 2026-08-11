package com.xiongx9527.fw.plus.boot.starter.magicapi;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicApiDbTxResourceTest {

	@Test
	void mysqlUndoLogMustShipWithTheMainMagicApiStarter() throws Exception {
		ClassPathResource resource = new ClassPathResource(
				"META-INF/sql/seata/mysql-undo-log.sql");
		assertTrue(resource.exists());
		String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
		assertTrue(sql.contains("CREATE TABLE `undo_log`"));
		assertTrue(sql.contains("UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)"));
	}
}
