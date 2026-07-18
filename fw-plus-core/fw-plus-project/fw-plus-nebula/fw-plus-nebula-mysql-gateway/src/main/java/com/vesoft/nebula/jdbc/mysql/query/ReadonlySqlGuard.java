package com.vesoft.nebula.jdbc.mysql.query;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class ReadonlySqlGuard {
	private static final Set<String> REJECTED_FIRST_KEYWORDS = new HashSet<String>(Arrays.asList(
			"INSERT", "UPDATE", "DELETE", "REPLACE", "CREATE", "ALTER", "DROP", "TRUNCATE", "RENAME",
			"GRANT", "REVOKE", "BEGIN", "COMMIT", "ROLLBACK", "CALL"
	));

	public void check(String sql) throws SQLException {
		String normalized = stripLeadingComments(sql).trim();
		if (normalized.length() == 0) {
			return;
		}
		String upper = normalized.toUpperCase(Locale.ENGLISH);
		String first = firstKeyword(upper);
		if (REJECTED_FIRST_KEYWORDS.contains(first)
				|| upper.startsWith("START TRANSACTION")
				|| upper.startsWith("CREATE PROCEDURE")
				|| upper.startsWith("CREATE FUNCTION")
				|| upper.startsWith("CREATE TRIGGER")
				|| upper.startsWith("SHOW BINLOG")
				|| upper.startsWith("SHOW MASTER")
				|| upper.startsWith("SHOW SLAVE")
				|| upper.startsWith("CHANGE MASTER")) {
			throw MysqlErrorMapper.readOnly("Read-only Nebula MySQL gateway does not support statement: " + first);
		}
	}

	private String firstKeyword(String sql) {
		int end = 0;
		while (end < sql.length() && Character.isLetter(sql.charAt(end))) {
			end++;
		}
		return end == 0 ? "" : sql.substring(0, end);
	}

	private String stripLeadingComments(String sql) {
		String value = sql == null ? "" : sql;
		boolean changed = true;
		while (changed) {
			changed = false;
			value = value.trim();
			if (value.startsWith("/*")) {
				int end = value.indexOf("*/");
				if (end >= 0) {
					value = value.substring(end + 2);
					changed = true;
				}
			} else if (value.startsWith("--")) {
				int end = value.indexOf('\n');
				value = end >= 0 ? value.substring(end + 1) : "";
				changed = true;
			}
		}
		return value;
	}
}
