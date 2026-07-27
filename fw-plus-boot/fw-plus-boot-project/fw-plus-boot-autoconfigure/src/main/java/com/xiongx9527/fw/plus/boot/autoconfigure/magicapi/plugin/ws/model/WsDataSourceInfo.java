package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class WsDataSourceInfo extends MagicEntity {
	/**
	 * 数据源key
	 */
	private String key;

	/**
	 * 密钥
	 */
	private String secret;

	/**
	 * 描述
	 */
	private String description;

	/**
	 * 其它配置
	 */
	private Map<String, Object> properties = new LinkedHashMap<String, Object>();

	@Override
	public MagicEntity simple() {
		WsDataSourceInfo dataSourceInfo = new WsDataSourceInfo();
		super.simple(dataSourceInfo);
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setDescription(this.description);
		dataSourceInfo.setSecret(this.secret);
		dataSourceInfo.setProperties(copyProperties());
		return dataSourceInfo;
	}

	@Override
	public MagicEntity copy() {
		WsDataSourceInfo dataSourceInfo = new WsDataSourceInfo();
		super.copyTo(dataSourceInfo);
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setDescription(this.description);
		dataSourceInfo.setSecret(this.secret);
		dataSourceInfo.setProperties(copyProperties());
		return dataSourceInfo;
	}

	private Map<String, Object> copyProperties() {
		return this.properties == null
				? new LinkedHashMap<String, Object>()
				: new LinkedHashMap<String, Object>(this.properties);
	}
}
