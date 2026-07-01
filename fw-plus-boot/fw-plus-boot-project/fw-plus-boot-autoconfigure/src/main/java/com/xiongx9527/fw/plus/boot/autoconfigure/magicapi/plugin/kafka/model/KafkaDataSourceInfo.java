package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;

@Data
public class KafkaDataSourceInfo extends MagicEntity {

	/**
	 * 数据源key
	 */
	private String key;

	/**
	 * 服务器地址
	 */
	private String servers;

	/**
	 * 其他配置
	 */
	private String other;


	/**
	 * 创建并配置Kafka数据源信息对象
	 *
	 * @return 配置完成的Kafka数据源信息对象
	 */
	@Override
	public MagicEntity simple() {
		// 创建Kafka数据源信息对象
		KafkaDataSourceInfo dataSourceInfo = new KafkaDataSourceInfo();
		super.simple(dataSourceInfo);
		// 设置Kafka连接所需的基本配置信息
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setServers(this.servers);
		dataSourceInfo.setOther(this.other);
		return dataSourceInfo;
	}


	/**
	 * 创建当前Kafka数据源信息对象的深拷贝副本
	 *
	 * @return 返回一个新的KafkaDataSourceInfo对象，包含与当前对象相同的数据
	 */
	@Override
	public MagicEntity copy() {
		// 创建新的Kafka数据源信息对象
		KafkaDataSourceInfo dataSourceInfo = new KafkaDataSourceInfo();
		// 复制父类中的通用属性
		super.copyTo(dataSourceInfo);
		// 复制Kafka特有的属性
		dataSourceInfo.setKey(this.key);
		dataSourceInfo.setServers(this.servers);
		dataSourceInfo.setOther(this.other);
		return dataSourceInfo;
	}

}
