package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MagicJdbcClassOptions {

	private final List<String> jdbcDrivers;

	private final List<String> datasourceTypes;

	public MagicJdbcClassOptions(List<String> jdbcDrivers, List<String> datasourceTypes) {
		this.jdbcDrivers = Collections.unmodifiableList(new ArrayList<String>(jdbcDrivers));
		this.datasourceTypes = Collections.unmodifiableList(new ArrayList<String>(datasourceTypes));
	}

	public List<String> getJdbcDrivers() {
		return jdbcDrivers;
	}

	public List<String> getDatasourceTypes() {
		return datasourceTypes;
	}
}
