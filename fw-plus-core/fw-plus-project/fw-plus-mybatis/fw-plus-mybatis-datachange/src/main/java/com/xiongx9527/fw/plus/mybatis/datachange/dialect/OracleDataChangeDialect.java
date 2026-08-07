package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

public final class OracleDataChangeDialect extends AbstractStandardDataChangeDialect {
	@Override
	public boolean supports(DatabaseMetadata metadata) {
		return metadata != null && "oracle".equals(metadata.getProductName());
	}

	@Override
	protected void appendLimit(StringBuilder sql, ChangeTarget target, boolean hasWhere) {
		sql.append(hasWhere ? " AND " : " WHERE ").append("ROWNUM <= ").append(queryLimit(target));
	}

	@Override
	protected String quote(String identifier) {
		String value = identifier == null ? "" : identifier;
		if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
			value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
		} else {
			value = value.toUpperCase(java.util.Locale.ENGLISH);
		}
		return '"' + value.replace("\"", "\"\"") + '"';
	}
}
