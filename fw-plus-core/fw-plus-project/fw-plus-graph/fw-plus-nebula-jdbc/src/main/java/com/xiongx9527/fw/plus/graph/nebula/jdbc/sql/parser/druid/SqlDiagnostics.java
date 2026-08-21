package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.druid;

final class SqlDiagnostics {

	private static final int MAX_SNIPPET_LENGTH = 160;

	private SqlDiagnostics() {
	}

	static String snippet(String sql) {
		if (sql == null) {
			return null;
		}
		String compact = sql.replaceAll("\\s+", " ").trim();
		if (compact.length() <= MAX_SNIPPET_LENGTH) {
			return compact;
		}
		return compact.substring(0, MAX_SNIPPET_LENGTH) + "...";
	}
}
