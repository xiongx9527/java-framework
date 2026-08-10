package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service;

import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ManagedMagicDynamicDataSource extends MagicDynamicDataSource {

	public synchronized DataSourceNode putAndGet(String id, String key, String name,
			DataSource dataSource, int maxRows) {
		super.put(id, key, name, dataSource, maxRows);
		return super.getDataSource(key);
	}

	@Override
	public synchronized void put(DataSource dataSource) {
		super.put(dataSource);
	}

	@Override
	public synchronized void put(String key, DataSource dataSource) {
		super.put(key, dataSource);
	}

	@Override
	public synchronized void put(String key, DataSource dataSource, int maxRows) {
		super.put(key, dataSource, maxRows);
	}

	@Override
	public synchronized void put(String id, String key, String name,
			DataSource dataSource, int maxRows) {
		super.put(id, key, name, dataSource, maxRows);
	}

	@Override
	public synchronized boolean delete(String key) {
		return super.delete(key);
	}

	@Override
	public synchronized DataSourceNode getDataSource() {
		return super.getDataSource();
	}

	@Override
	public synchronized DataSourceNode getDataSource(String key) {
		return super.getDataSource(key);
	}

	@Override
	public synchronized Collection<DataSourceNode> datasourceNodes() {
		return new ArrayList<DataSourceNode>(super.datasourceNodes());
	}

	@Override
	public synchronized List<String> datasources() {
		return new ArrayList<String>(super.datasources());
	}

	@Override
	public synchronized boolean isEmpty() {
		return super.isEmpty();
	}
}
