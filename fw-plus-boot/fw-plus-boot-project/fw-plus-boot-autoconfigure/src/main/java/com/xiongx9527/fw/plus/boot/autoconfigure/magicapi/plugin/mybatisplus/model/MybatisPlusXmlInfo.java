package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mybatisplus.model;

import org.ssssssss.magicapi.core.model.PathMagicEntity;

public class MybatisPlusXmlInfo extends PathMagicEntity {

	private Boolean enabled = Boolean.TRUE;
	private String dbKey;
	private String tableName;

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isEnabled() {
		return enabled == null || enabled;
	}

	public String getDbKey() {
		return dbKey;
	}

	public void setDbKey(String dbKey) {
		this.dbKey = dbKey;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	@Override
	public MybatisPlusXmlInfo simple() {
		MybatisPlusXmlInfo info = new MybatisPlusXmlInfo();
		super.simple(info);
		info.setEnabled(getEnabled());
		info.setDbKey(getDbKey());
		info.setTableName(getTableName());
		return info;
	}

	@Override
	public MybatisPlusXmlInfo copy() {
		MybatisPlusXmlInfo info = new MybatisPlusXmlInfo();
		return copyTo(info);
	}

	public <T extends MybatisPlusXmlInfo> T copyTo(T info) {
		super.copyTo(info);
		info.setEnabled(getEnabled());
		info.setDbKey(getDbKey());
		info.setTableName(getTableName());
		return info;
	}
}
