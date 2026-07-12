package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;

@Data
public class PulsarDataSourceInfo extends MagicEntity {

	/**
	 * 服务器地址
	 */
	private String servers;

	/**
	 * 服务http地址
	 */
	private String serviceUrls;

	/**
	 * 用户名
	 */
	//private String username;

	/**
	 * 密码
	 */
	//private String password;

	/**
	 * 数据源key
	 */
	private String key;

	/**
	 * 租户
	 */
	private String tenant;

	/**
	 * 命名空间
	 */
	private String namespace;

	/**
	 * 其他配置
	 */
	private String other;

	/**
	 * 创建一个简单的数据源信息对象副本
	 * <p>
	 * 该方法创建一个新的PulsarDataSourceInfo实例，并将当前对象的基本属性复制到新实例中。
	 * 首先调用父类的simple方法进行基础属性的设置，然后设置Pulsar特有的属性。
	 *
	 * @return MagicEntity 返回配置好的数据源信息对象
	 */
	@Override
	public MagicEntity simple() {
		PulsarDataSourceInfo dataSourceInfo = new PulsarDataSourceInfo();
		super.simple(dataSourceInfo);
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setTenant(this.tenant);
		dataSourceInfo.setNamespace(this.namespace);
		dataSourceInfo.setOther(this.other);
		dataSourceInfo.setServers(this.servers);
		dataSourceInfo.setServiceUrls(this.serviceUrls);
		return dataSourceInfo;
	}

	/**
	 * 创建一个完整的数据源信息对象副本
	 * <p>
	 * 该方法创建一个新的PulsarDataSourceInfo实例，并将当前对象的所有属性复制到新实例中。
	 * 与simple方法不同，这里调用父类的copyTo方法来处理基础属性的复制，确保更全面的克隆操作。
	 *
	 * @return MagicEntity 返回完整复制的数据源信息对象
	 */
	@Override
	public MagicEntity copy() {
		PulsarDataSourceInfo dataSourceInfo = new PulsarDataSourceInfo();
		super.copyTo(dataSourceInfo);
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setTenant(this.tenant);
		dataSourceInfo.setNamespace(this.namespace);
		dataSourceInfo.setOther(this.other);
		dataSourceInfo.setServers(this.servers);
		dataSourceInfo.setServiceUrls(this.serviceUrls);
		return dataSourceInfo;
	}

}
