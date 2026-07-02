package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model;


import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.LocalLockModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.Lock;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.consumer.RocketMQConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.consumer.RocketMqConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.producer.RocketMqProducer;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RocketMq 动态数据源对象
 *
 * @author tc
 */
@Slf4j
public class MagicDynamicRocketMqDataSource {

	private final Map<String, RocketMqProducer> producerMap = new ConcurrentHashMap<>();

	private final Map<String, RocketMQConsumer> consumerMap = new ConcurrentHashMap<>();

	private final RocketMqConsumerService rocketMqConsumerService;

	/** 分布式锁 */
	private final Lock lock  = new LocalLockModule();

	public MagicDynamicRocketMqDataSource(RocketMqConsumerService rocketMqConsumerService) {
		this.rocketMqConsumerService = rocketMqConsumerService;
	}

	/**
	 * 注册生产者数据源（可以运行时注册）
	 *
	 * @param info 数据源信息
	 */
	public boolean putProducer(RocketMqDataSourceInfo info) {
		try {
			this.producerMap.put(info.getKey(), new RocketMqProducer(info));
			log.info("注册 RocketMq 消息队列:[{}]完成执行", info.getKey());
		} catch (Exception e) {
			log.error("RocketMq 消息队列:[{}]注册失败", info.getKey());
			return false;
		}

		return true;
	}

	/**
	 * 注册消费者数据源（可以运行时注册）
	 *
	 * @param info 数据源信息
	 */
	public boolean putConsumer(RocketMqDataSourceInfo info, List<RocketMqSubscribeScriptInfo> subscribeScripteInfoList) {
		Object lockInfo = null;
		try {
			lockInfo = lock.lock(info.getKey(), 5000, 5000);
			RocketMQConsumer rocketMQConsumer = this.consumerMap.get(info.getKey());
			if (rocketMQConsumer != null) {
				rocketMQConsumer.createConsumer(subscribeScripteInfoList);
			} else {
				rocketMQConsumer = new RocketMQConsumer(this.rocketMqConsumerService, info, subscribeScripteInfoList);
				this.consumerMap.put(info.getKey(), rocketMQConsumer);
			}
			log.info("注册 RocketMq 消息队列:[{}]完成执行", info.getKey());
		} catch (Exception e) {
			log.error("RocketMq 消息队列:[{}]注册失败", info.getKey());
			return false;
		} finally {
			if (lockInfo != null) {
				lock.release(lockInfo);
			}
		}
		return true;
	}

	/**
	 * 删除生产者数据源
	 *
	 * @param datasourceKey 数据源Key
	 */
	public void deleteProducer(String datasourceKey) {
		// 检查参数是否合法
		if (datasourceKey != null && !datasourceKey.isEmpty()) {
			RocketMqProducer producer = this.producerMap.remove(datasourceKey);
			if (producer != null) {
				producer.close();
			}
		}
	}

	/**
	 * 获取全部生产者数据源
	 */
	public Collection<RocketMqProducer> producerNodes() {
		return this.producerMap.values();
	}

	/**
	 * 获取消费者数据源
	 *
	 * @param datasourceKey 数据源Key
	 */
	public RocketMQConsumer getConsumer(String datasourceKey) {
		RocketMQConsumer dataSourceNode = this.consumerMap.get(datasourceKey);
		return dataSourceNode;
	}

	public RocketMqProducer getProducer(String datasourceKey) {
		RocketMqProducer dataSourceNode = this.producerMap.get(datasourceKey);
		return dataSourceNode;
	}

}
