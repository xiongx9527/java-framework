package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

public final class SqlServerDataChangeDialect extends AbstractStandardDataChangeDialect {
	@Override
	public boolean supports(DatabaseMetadata metadata) {
		return metadata != null && ("microsoft sql server".equals(metadata.getProductName())
				|| "sql server".equals(metadata.getProductName()));
	}

	@Override
	protected String selectKeyword(ChangeTarget target) {
		return "SELECT TOP (" + queryLimit(target) + ") ";
	}

	@Override
	protected void appendTableLock(StringBuilder sql, boolean lockRows) {
		if (lockRows) { sql.append(" WITH (UPDLOCK, HOLDLOCK)"); }
	}

	@Override
	protected void appendLimit(StringBuilder sql, ChangeTarget target, boolean hasWhere) { }

	@Override
	protected void appendRowLock(StringBuilder sql) { }

	@Override
	protected String quote(String identifier) {
		String value = identifier == null ? "" : identifier;
		if (value.length() >= 2 && value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']') {
			value = value.substring(1, value.length() - 1).replace("]]", "]");
		}
		return '[' + value.replace("]", "]]" ) + ']';
	}
}
