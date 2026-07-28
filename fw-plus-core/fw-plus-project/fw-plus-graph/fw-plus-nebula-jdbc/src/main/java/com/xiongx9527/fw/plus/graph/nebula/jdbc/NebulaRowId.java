package com.xiongx9527.fw.plus.graph.nebula.jdbc;

import java.nio.charset.StandardCharsets;
import java.sql.RowId;
import java.util.Arrays;

public class NebulaRowId implements RowId {
	private final byte[] value;

	public NebulaRowId(byte[] value) {
		this.value = value == null ? new byte[0] : Arrays.copyOf(value, value.length);
	}

	public NebulaRowId(String value) {
		this(value == null ? null : value.getBytes(StandardCharsets.UTF_8));
	}

	@Override
	public byte[] getBytes() {
		return Arrays.copyOf(value, value.length);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof RowId)) {
			return false;
		}
		return Arrays.equals(value, ((RowId) obj).getBytes());
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(value);
	}

	@Override
	public String toString() {
		return new String(value, StandardCharsets.UTF_8);
	}
}
