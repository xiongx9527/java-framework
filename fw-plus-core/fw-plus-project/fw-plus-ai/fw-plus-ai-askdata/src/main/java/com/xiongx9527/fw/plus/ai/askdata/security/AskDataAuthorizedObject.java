package com.xiongx9527.fw.plus.ai.askdata.security;

/**
 * AskData 已授权的数据源、表和列对象。
 */
public final class AskDataAuthorizedObject {

	private final String datasourceKey;
	private final String tableName;
	private final String columnName;

	public AskDataAuthorizedObject(String datasourceKey, String tableName, String columnName) {
		this.datasourceKey = requireNonBlank(datasourceKey, "datasourceKey");
		this.tableName = requireNonBlank(tableName, "tableName");
		this.columnName = requireNonBlank(columnName, "columnName");
	}

	public String getDatasourceKey() {
		return datasourceKey;
	}

	public String getTableName() {
		return tableName;
	}

	public String getColumnName() {
		return columnName;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AskDataAuthorizedObject)) {
			return false;
		}
		AskDataAuthorizedObject that = (AskDataAuthorizedObject) other;
		return datasourceKey.equals(that.datasourceKey) && tableName.equals(that.tableName)
				&& columnName.equals(that.columnName);
	}

	@Override
	public int hashCode() {
		int result = datasourceKey.hashCode();
		result = 31 * result + tableName.hashCode();
		result = 31 * result + columnName.hashCode();
		return result;
	}

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(fieldName + " 不能为空白");
		}
		return value.trim();
	}
}
