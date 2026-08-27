package com.xiongx9527.fw.plus.mybatis.datachange.adapter;

import java.util.Objects;

public final class FrameworkFieldMetadata {
	private final String property;
	private final String column;

	public FrameworkFieldMetadata(String property, String column) {
		this.property = property;
		this.column = column;
	}

	public String getProperty() { return property; }
	public String getColumn() { return column; }

	@Override
	public boolean equals(Object other) {
		if (this == other) { return true; }
		if (!(other instanceof FrameworkFieldMetadata)) { return false; }
		FrameworkFieldMetadata that = (FrameworkFieldMetadata) other;
		return Objects.equals(property, that.property) && Objects.equals(column, that.column);
	}

	@Override
	public int hashCode() {
		return Objects.hash(property, column);
	}
}
