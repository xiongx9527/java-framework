package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

public final class PostgreSqlDataChangeDialect extends AbstractStandardDataChangeDialect {
	@Override
	public boolean supports(DatabaseMetadata metadata) {
		return metadata != null && "postgresql".equals(metadata.getProductName());
	}

	@Override
	protected String quote(String identifier) {
		String value = identifier == null ? "" : identifier;
		if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
			value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
		} else {
			value = value.toLowerCase(java.util.Locale.ENGLISH);
		}
		return '"' + value.replace("\"", "\"\"") + '"';
	}
}
