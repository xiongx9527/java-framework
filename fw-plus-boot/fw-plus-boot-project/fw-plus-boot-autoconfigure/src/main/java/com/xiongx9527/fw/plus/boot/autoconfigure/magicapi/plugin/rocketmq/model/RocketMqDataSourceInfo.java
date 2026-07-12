package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;

@Data
public class RocketMqDataSourceInfo extends MagicEntity {

	/**
	 * 服务器地址
	 */
	private String nameServer;

	/**
	 * 用户名
	 */
	private String accessKey;

	/**
	 * 密码
	 */
	private String secretKey;

	/**
	 * 数据源key
	 */
	private String key;

	/**
	 * 消费组
	 */
	private String group = "magic-api";

	/**
	 * 其他配置
	 */
	private String other;

	@Override
	public MagicEntity simple() {
		RocketMqDataSourceInfo dataSourceInfo = new RocketMqDataSourceInfo();
		super.simple(dataSourceInfo);
		dataSourceInfo.setAccessKey(this.accessKey);
		dataSourceInfo.setSecretKey(this.secretKey);
		dataSourceInfo.setNameServer(this.nameServer);
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setOther(this.other);
		dataSourceInfo.setGroup(this.group);
		return dataSourceInfo;
	}

	@Override
	public MagicEntity copy() {
		RocketMqDataSourceInfo dataSourceInfo = new RocketMqDataSourceInfo();
		super.copyTo(dataSourceInfo);
		dataSourceInfo.setAccessKey(this.accessKey);
		dataSourceInfo.setSecretKey(this.secretKey);
		dataSourceInfo.setNameServer(this.nameServer);
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setOther(this.other);
		dataSourceInfo.setGroup(this.group);
		return dataSourceInfo;
	}
}
