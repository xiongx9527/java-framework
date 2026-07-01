package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;

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
	private String other;

	@Override
	public MagicEntity simple() {
		WsDataSourceInfo dataSourceInfo = new WsDataSourceInfo();
		super.simple(dataSourceInfo);
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setDescription(this.description);
		dataSourceInfo.setSecret(this.secret);
		dataSourceInfo.setOther(this.other);
		return dataSourceInfo;
	}

	@Override
	public MagicEntity copy() {
		WsDataSourceInfo dataSourceInfo = new WsDataSourceInfo();
		super.copyTo(dataSourceInfo);
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setDescription(this.description);
		dataSourceInfo.setSecret(this.secret);
		dataSourceInfo.setOther(this.other);
		return dataSourceInfo;
	}
}
