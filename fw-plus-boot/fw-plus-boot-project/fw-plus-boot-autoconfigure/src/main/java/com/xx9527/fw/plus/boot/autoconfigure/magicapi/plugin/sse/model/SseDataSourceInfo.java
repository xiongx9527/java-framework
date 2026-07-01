package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;

@Data
public class SseDataSourceInfo extends MagicEntity {
	/**
	 * 数据源key
	 */
	private String key;

	/**
	 * 密钥
	 */
	private String secret;

	/**
	 * 是否mcp
	 */
	private boolean mcp  = false;

	/**
	 * 描述
	 */
	private String description;

	/**
	 * 其它配置
	 */
	private String other;

	/**
	 * 创建数据源信息的简单副本
	 * <p>
	 * 该方法创建一个新的SseDataSourceInfo对象，调用父类的simple方法进行基础初始化，
	 * 然后设置当前对象的key、description和secret属性到新创建的对象中。
	 * </p>
	 *
	 * @return MagicEntity 返回包含当前对象属性的新SseDataSourceInfo实例
	 */
	@Override
	public MagicEntity simple() {
		SseDataSourceInfo dataSourceInfo = new SseDataSourceInfo();
		super.simple(dataSourceInfo);
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setDescription(this.description);
		dataSourceInfo.setSecret(this.secret);
		dataSourceInfo.setMcp(this.mcp);
		return dataSourceInfo;
	}

	/**
	 * 创建数据源信息的完整副本
	 * <p>
	 * 该方法创建一个新的SseDataSourceInfo对象，调用父类的copyTo方法进行复制，
	 * 然后设置当前对象的key、description和secret属性到新创建的对象中。
	 * </p>
	 *
	 * @return MagicEntity 返回包含当前对象所有属性的新SseDataSourceInfo实例
	 */
	@Override
	public MagicEntity copy() {
		SseDataSourceInfo dataSourceInfo = new SseDataSourceInfo();
		super.copyTo(dataSourceInfo);
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setDescription(this.description);
		dataSourceInfo.setSecret(this.secret);
		dataSourceInfo.setMcp(this.mcp);
		return dataSourceInfo;
	}


}
