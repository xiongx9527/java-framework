package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import java.util.Locale;
import java.util.Objects;

public final class DatabaseMetadata {
	private final String productName;
	private final int majorVersion;
	private final int minorVersion;

	public DatabaseMetadata(String productName, int majorVersion, int minorVersion) {
		this.productName = productName == null ? "" : productName.trim().toLowerCase(Locale.ENGLISH);
		this.majorVersion = majorVersion;
		this.minorVersion = minorVersion;
	}

	public String getProductName() { return productName; }
	public int getMajorVersion() { return majorVersion; }
	public int getMinorVersion() { return minorVersion; }

	@Override
	public boolean equals(Object other) {
		if (this == other) { return true; }
		if (!(other instanceof DatabaseMetadata)) { return false; }
		DatabaseMetadata that = (DatabaseMetadata) other;
		return majorVersion == that.majorVersion && minorVersion == that.minorVersion
				&& Objects.equals(productName, that.productName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(productName, majorVersion, minorVersion);
	}
}
