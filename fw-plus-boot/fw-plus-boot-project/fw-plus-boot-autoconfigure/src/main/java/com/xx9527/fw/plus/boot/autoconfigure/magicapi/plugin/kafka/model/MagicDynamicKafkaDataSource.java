package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer.KafkaConsumer;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.consumer.KafkaConsumerService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.producer.KafkaProducer;
import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kafka 动态数据源对象
 *
 * @author tc
 */
@Slf4j
public class MagicDynamicKafkaDataSource implements DisposableBean {

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

	/**
	 * 默认的Kafka AdminClient实例
	 */
	private volatile AdminClient defaultAdminClient;

	/** DCL 专用锁对象，避免 synchronized(servers) 参数锁导致线程不安全 */
	private final Object adminClientLock = new Object();

	/**
	 * 存储所有Kafka AdminClient实例的映射
	 * key为AdminClient数据源的唯一标识，value为对应的AdminClient实例
	 */
	private final Map<String, AdminClient> adminClientMap = new ConcurrentHashMap<>();

	/**
	 * Kafka消费者服务
	 */
	private final KafkaConsumerService kafkaConsumerService;

	/**
	 * 构造函数，初始化Kafka动态数据源
	 *
	 * @param kafkaConsumerService Kafka消费者服务
	 */
	public MagicDynamicKafkaDataSource(KafkaConsumerService kafkaConsumerService) {
		this.kafkaConsumerService = kafkaConsumerService;
    }


	/**
	 * 获取默认的Kafka AdminClient实例
	 *
	 * @return Kafka AdminClient实例，如果未初始化则返回null
	 */
	public AdminClient getDefaultAdminClient() {
		return this.defaultAdminClient;
	}

	/**
	 * 创建并初始化默认的Kafka AdminClient实例
	 * 使用双重检查锁定（DCL）确保在并发环境下只初始化一次
	 *
	 * @param servers Kafka集群服务器地址列表
	 */
	public void  createDefaultAdminClient(String servers) {
		if (this.defaultAdminClient == null) {
			synchronized (adminClientLock) {
				if (this.defaultAdminClient == null) {
					Map<String, Object> props = new HashMap<>(32);
					props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers);
					this.defaultAdminClient = AdminClient.create(props);
				}
			}
		}
	}

	/**
	 * 创建并缓存Kafka AdminClient实例
	 * 如果已存在对应的AdminClient，则先关闭旧实例再替换为新实例，防止资源泄漏
	 *
	 * @param info Kafka数据源配置信息，包含key和servers等必要配置
	 * @return 创建成功返回true，失败返回false（如配置缺失、生产者不存在等）
	 */
	public boolean putAdminClient(KafkaDataSourceInfo info) {
		if (info == null || StringUtils.isBlank(info.getKey())) {
			return false;
		}
		if (this.adminClientMap.containsKey(info.getKey())) {
			return true;
		}
		KafkaProducer kafkaProducer = this.producerMap.get(info.getKey());
		if (kafkaProducer == null) {
			return false;
		}
		synchronized (this.adminClientMap) {
			if (this.adminClientMap.containsKey(info.getKey())) {
				return true;
			}
			try {
				// 优先从数据源信息获取服务器地址，避免依赖 ProducerFactory 内部实现
				String servers = info.getServers();
				servers = MagicAPIConfigUtils.getKafkaConfig(info.getKey(), "servers", servers);
				if (StringUtils.isBlank(servers)) {
					// 降级方案：尝试从 ProducerFactory 获取
					ProducerFactory<?, ?> producerFactory = kafkaProducer.getProducer().getProducerFactory();
					if (producerFactory != null) {
						Map<String, Object> configs = producerFactory.getConfigurationProperties();
						if (configs != null) {
							servers = (String) configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG);
						}
					}
				}

				if (StringUtils.isBlank(servers)) {
					return false;
				}

				Map<String, Object> props = new HashMap<>(32);
				props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, servers);

				AdminClient newAdminClient = AdminClient.create(props);

				// 关闭已存在的旧 AdminClient，防止资源泄漏
				AdminClient oldAdminClient = this.adminClientMap.put(info.getKey(), newAdminClient);
				if (oldAdminClient != null) {
					try {
						oldAdminClient.close();
					} catch (Exception e) {
						log.warn("关闭旧的 Kafka AdminClient 时发生异常", e);
					}
				}

				return true;
			} catch (Exception e) {
				log.error("创建 Kafka AdminClient 失败: {}", info.getKey(), e);
				return false;
			}
		}
	}

	/**
	 * 注册生产者数据源（可以运行时注册）
	 *
	 * @param info 数据源信息
	 */
	public boolean putProducer(KafkaDataSourceInfo info) {
		synchronized (this.producerMap) {
			try {
				KafkaProducer oldkafkaProducer = this.producerMap.put(info.getKey(), new KafkaProducer(info));
				if (null != oldkafkaProducer) {
					oldkafkaProducer.close();
				}
				log.info("注册 Kafka 生产者:[{}]完成执行", info.getKey());
			} catch (Throwable e) {
				log.error("Kafka 生产者:[{}]注册失败", info.getKey(), e);
				return false;
			}
		}
		return true;
	}

	/**
	 * 注册消费者数据源（可以运行时注册）
	 *
	 * @param info 数据源信息
	 */
	public boolean putConsumer(KafkaDataSourceInfo info, List<KafkaSubscribeScriptInfo> subscribeScripteInfoList) {
		synchronized (this.consumerMap) {
			try {
				KafkaConsumer kafkaConsumer = this.consumerMap.get(info.getKey());
				if (kafkaConsumer != null) {
					kafkaConsumer.createConsumer(subscribeScripteInfoList);
				} else {
					kafkaConsumer = new KafkaConsumer(this.kafkaConsumerService, info, subscribeScripteInfoList, this);
					this.consumerMap.put(info.getKey(), kafkaConsumer);
				}
				log.info("注册 Kafka 消费者:[{}]完成执行", info.getKey());
			} catch (Throwable e) {
				log.error("Kafka 消费者:[{}]注册失败", info.getKey(), e);
				return false;
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
		if (datasourceKey == null || datasourceKey.isEmpty()) {
			return;
		}
		// 分别在各自的锁保护下清理资源，避免与 putAdminClient(adminClientMap) 产生竞态
		synchronized (this.producerMap) {
			KafkaProducer producer = this.producerMap.remove(datasourceKey);
			if (producer != null) {
				producer.close();
			}
		}
		synchronized (this.adminClientMap) {
			AdminClient adminClient = this.adminClientMap.remove(datasourceKey);
			if (adminClient != null) {
				try {
					adminClient.close();
				} catch (Exception e) {
					log.warn("关闭 Kafka AdminClient {} 时发生异常", datasourceKey, e);
				}
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
			log.debug("获取默认KafkaTemplate失败，继续使用已有生产者列表", e);
		}
		return producers;
	}

	/**
	 * 根据数据源键获取Kafka AdminClient实例
	 *
	 * @param datasourceKey 数据源键
	 * @return Kafka AdminClient实例，如果不存在则返回null
	 */
	public AdminClient getAdminClient(String datasourceKey) {
		AdminClient adminClient = this.adminClientMap.get(datasourceKey);
		return adminClient;
	}

	/**
	 * 获取生产者数据源
	 *
	 * @param datasourceKey 数据源Key
	 */
	public KafkaProducer getProducer(String datasourceKey) {
		KafkaProducer dataSourceNode = this.producerMap.get(datasourceKey);
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

	/**
	 * 销毁数据源，关闭所有Kafka生产者和消费者连接
	 * 该方法会在应用 shutdown 时自动调用，确保资源正确释放
	 *
	 * @throws Exception 当关闭连接过程中发生异常时抛出
	 */
	@Override
	public void destroy() throws Exception {
		log.info("开始关闭所有 Kafka 生产者和消费者连接...");

		// 1. 先关闭所有消费者（停止消费新消息）
		synchronized (this.consumerMap) {
			for (Map.Entry<String, KafkaConsumer> entry : this.consumerMap.entrySet()) {
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
			this.consumerMap.clear();
		}

		// 2. 再关闭所有生产者（停止发送新消息）
		synchronized (this.producerMap) {
			for (Map.Entry<String, KafkaProducer> entry : this.producerMap.entrySet()) {
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
			this.producerMap.clear();
		}

		// 3. 最后关闭 AdminClient（清理管理连接）
		synchronized (this.adminClientMap) {
			for (Map.Entry<String, AdminClient> entry : this.adminClientMap.entrySet()) {
				try {
					AdminClient adminClient = entry.getValue();
					if (adminClient != null) {
						adminClient.close();
						log.info("已关闭 Kafka AdminClient: {}", entry.getKey());
					}
				} catch (Exception e) {
					log.error("关闭 Kafka AdminClient {} 时发生异常", entry.getKey(), e);
				}
			}
			this.adminClientMap.clear();
		}
		
		// 4. 关闭默认 AdminClient
		if (this.defaultAdminClient != null) {
			try {
				this.defaultAdminClient.close();
			} catch (Exception e) {
				log.warn("关闭默认 Kafka AdminClient 时发生异常", e);
			}
			this.defaultAdminClient = null;
		}

		log.info("所有 Kafka 连接已关闭");
	}
}
