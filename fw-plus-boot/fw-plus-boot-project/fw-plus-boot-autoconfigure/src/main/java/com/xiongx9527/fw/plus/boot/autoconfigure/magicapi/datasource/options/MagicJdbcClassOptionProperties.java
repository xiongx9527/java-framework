package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "fw.plus.magicapi.jdbc")
public class MagicJdbcClassOptionProperties {

	private List<String> driverCandidates = new ArrayList<String>();

	private List<String> datasourceTypeCandidates = new ArrayList<String>();

	public List<String> getDriverCandidates() {
		return driverCandidates;
	}

	public void setDriverCandidates(List<String> driverCandidates) {
		this.driverCandidates = driverCandidates == null ? new ArrayList<String>() : driverCandidates;
	}

	public List<String> getDatasourceTypeCandidates() {
		return datasourceTypeCandidates;
	}

	public void setDatasourceTypeCandidates(List<String> datasourceTypeCandidates) {
		this.datasourceTypeCandidates = datasourceTypeCandidates == null ? new ArrayList<String>() : datasourceTypeCandidates;
	}
}
