package com.xiongx9527.fw.plus.ai.askdata.sql;

/**
 * SQL 结构化结果中的中立列描述。
 */
public final class AskDataSqlColumn {

	private final String name;
	private final String label;
	private final String type;

	public AskDataSqlColumn(String name, String label, String type) {
		if (name == null || name.trim().length() == 0) {
			throw new IllegalArgumentException("name 不能为空白");
		}
		this.name = name.trim();
		this.label = label == null ? null : label.trim();
		this.type = type == null ? null : type.trim();
	}

	public String getName() {
		return name;
	}

	public String getLabel() {
		return label;
	}

	public String getType() {
		return type;
	}
}
