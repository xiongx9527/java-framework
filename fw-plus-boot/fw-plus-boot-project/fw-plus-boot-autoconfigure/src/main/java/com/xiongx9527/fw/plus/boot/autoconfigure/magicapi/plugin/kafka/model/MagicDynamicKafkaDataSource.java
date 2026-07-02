package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.LocalLockModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.Lock;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer.KafkaConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer.KafkaConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.producer.KafkaProducer;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.ssssssss.magicapi.utils.Assert;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka 动态数据源对象
 *
 * @author tc
 */
@Slf4j
public class MagicDynamicKafkaDataSource implements DisposableBean  {

	/**
	 * 存储所有Kafka生产者实例的映射
	 * key为生产者数据源的唯一标识，value为对应的KafkaProducer实例
	 */
	private final Map<String, KafkaProducer> producerMap = new ConcurrentHashMap<>();

	/**
	 * 存储所有Kafka消费者实例的映射
	 * key为消费者数据源的唯一标识，value为对应的KafkaConsumer实例
	 */
	private final Map<String, KafkaConsumer> consumerMap = new ConcurrentHashMap<>();

	/** Kafka消费者服务 */
	private final KafkaConsumerService kafkaConsumerService;

	/** 分布式锁 */
	private final Lock lock  = new LocalLockModule();

	/**
	 * 构造函数，初始化Kafka动态数据源
	 *
	 * @param kafkaConsumerService Kafka消费者服务
	 */
	public MagicDynamicKafkaDataSource(KafkaConsumerService kafkaConsumerService) {
		this.kafkaConsumerService = kafkaConsumerService;
    }

	/**
	 * 注册生产者数据源（可以运行时注册）
	 *
	 * @param info 数据源信息
	 */
	public boolean putProducer(KafkaDataSourceInfo info) {
		try {
			this.producerMap.put(info.getKey(), new KafkaProducer(info));
			log.info("注册 Kafka 生产者:[{}]完成执行", info.getKey());
		} catch (Throwable e) {
			log.error("Kafka 生产者:[{}]注册失败", info.getKey(),e);
			return false;
		}

		return true;
	}

	/**
	 * 注册消费者数据源（可以运行时注册）
	 *
	 * @param info 数据源信息
	 */
	public boolean putConsumer(KafkaDataSourceInfo info, List<KafkaSubscribeScriptInfo> subscribeScripteInfoList) {
		Object lockInfo = null;
		try {
			lockInfo = lock.lock(info.getKey(), 5000, 5000);
			KafkaConsumer kafkaConsumer =  this.consumerMap.get(info.getKey());
			if (kafkaConsumer != null) {
				kafkaConsumer.createConsumer(subscribeScripteInfoList);
			} else {
				kafkaConsumer = new KafkaConsumer(this.kafkaConsumerService, info, subscribeScripteInfoList);
				this.consumerMap.put(info.getKey(), kafkaConsumer);
			}
			log.info("注册 Kafka 消费者:[{}]完成执行", info.getKey());
		} catch (Throwable e) {
			log.error("Kafka 消费者:[{}]注册失败", info.getKey(),e);
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
			KafkaProducer producer = this.producerMap.remove(datasourceKey);
			if (producer != null) {
				producer.close();
			}
		}
	}

	/**
	 * 获取全部生产者数据源
	 * 
	 * @return 包含所有生产者数据源的集合
	 */
	public Collection<KafkaProducer> producerNodes() {
		// 创建一个包含当前所有生产者的列表
		List<KafkaProducer> producers = new ArrayList<>(this.producerMap.values());
		try {
			// 尝试从Spring上下文中获取默认的KafkaTemplate
			KafkaTemplate kafkaTemplate = SpringUtils.getBean(KafkaTemplate.class);
			// 如果获取到默认的KafkaTemplate，则创建一个对应的生产者并添加到列表中
			if (kafkaTemplate != null) {
				producers.add(new KafkaProducer(kafkaTemplate));
			}
		} catch (Exception e) {
			// 忽略异常，继续使用已有的生产者列表
		}
		return producers;
	}

	/**
	 * 获取生产者数据源
	 *
	 * @param datasourceKey 数据源Key
	 */
	public KafkaProducer getProducer(String datasourceKey) {
		KafkaProducer dataSourceNode = producerMap.get(datasourceKey);
		Assert.isNotNull(dataSourceNode, String.format("找不到 Kafka 数据源%s", datasourceKey));
		return dataSourceNode;
	}

	/**
	 * 获取消费者数据源
	 *
	 * @param datasourceKey 数据源Key
	 */
	public KafkaConsumer getConsumer(String datasourceKey) {
		KafkaConsumer dataSourceNode = this.consumerMap.get(datasourceKey);
		return dataSourceNode;
	}

	@Override
	public void destroy() throws Exception {
		//关闭所有的生产者和消费者连接
		log.info("开始关闭所有 Kafka 生产者和消费者连接...");
		
		// 关闭所有生产者
		for (Map.Entry<String, KafkaProducer> entry : producerMap.entrySet()) {
			try {
				KafkaProducer producer = entry.getValue();
				if (producer != null) {
					producer.close();
					log.info("已关闭 Kafka 生产者: {}", entry.getKey());
				}
			} catch (Exception e) {
				log.error("关闭 Kafka 生产者 {} 时发生异常", entry.getKey(), e);
			}
		}
		producerMap.clear();
		
		// 关闭所有消费者
		for (Map.Entry<String, KafkaConsumer> entry : consumerMap.entrySet()) {
			try {
				KafkaConsumer consumer = entry.getValue();
				if (consumer != null) {
					consumer.close();
					log.info("已关闭 Kafka 消费者: {}", entry.getKey());
				}
			} catch (Exception e) {
				log.error("关闭 Kafka 消费者 {} 时发生异常", entry.getKey(), e);
			}
		}
		consumerMap.clear();
		
		log.info("所有 Kafka 连接已关闭");
	}
}
