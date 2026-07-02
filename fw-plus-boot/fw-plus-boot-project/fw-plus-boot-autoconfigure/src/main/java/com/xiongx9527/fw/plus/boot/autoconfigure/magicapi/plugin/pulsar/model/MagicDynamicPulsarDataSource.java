package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.LocalLockModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.Lock;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.consumer.PulsarConsumer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.consumer.PulsarConsumerService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.producer.PulsarProducer;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.shade.com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.DisposableBean;
import org.ssssssss.magicapi.utils.Assert;

import java.util.*;

/**
 * Pulsar 动态数据源对象
 *
 * @author tc
 */
@Slf4j
public class MagicDynamicPulsarDataSource implements DisposableBean {

	/**
	 * 存储所有Pulsar客户端实例的映射
	 * key为数据源的唯一标识，value为对应的PulsarClient实例
	 */
	private final Map<String, PulsarClient> pulsarClientMap = new HashMap<>();

	/**
	 * 存储所有Pulsar生产者实例的映射
	 * key为生产者数据源的唯一标识，value为对应的PulsarProducer实例
	 */
	private final Map<String, PulsarProducer> producerMap = new HashMap<>();

	/**
	 * 存储所有Pulsar消费者实例的映射
	 * key为消费者数据源的唯一标识，value为对应的PulsarConsumer实例
	 */
	private final Map<String, PulsarConsumer> consumerMap = new HashMap<>();

	/**
	 * Pulsar消费者服务实例
	 * 用于从Pulsar主题接收消息
	 */
	private final PulsarConsumerService pulsarConsumerService;


	/** 分布式锁 */
	private final Lock lock  = new LocalLockModule();

	/**
	 * 构造函数，初始化Pulsar动态数据源
	 *
	 * @param pulsarConsumerService Pulsar消费者服务
	 */
	public MagicDynamicPulsarDataSource(PulsarConsumerService pulsarConsumerService) {
		this.pulsarConsumerService = pulsarConsumerService;
	}

	/**
	 * 获取Pulsar消费者服务
	 *
	 * @return PulsarConsumerService Pulsar消费者服务实例
	 */
	public PulsarConsumerService getPulsarConsumerService() {
		return this.pulsarConsumerService;
	}

	/**
	 * 向pulsarClientMap中添加Pulsar客户端对象
	 * 使用双重检查锁定模式优化并发性能，避免重复创建客户端
	 *
	 * @param info Pulsar数据源信息，包含连接到Pulsar服务所需的信息
	 * @throws PulsarClientException 当创建Pulsar客户端对象时发生错误，将抛出此异常
	 */
	private void putPulsarClient(PulsarDataSourceInfo info) throws PulsarClientException {
		// 第一次检查，避免已经存在的key重复进入同步块
		if (this.pulsarClientMap.containsKey(info.getKey())) {
			return;
		}
		
		// 同步块确保只有一个线程能创建指定key的客户端
		synchronized (this.pulsarClientMap) {
			// 第二次检查，确保在获得锁之后再次确认是否需要创建客户端
			if (this.pulsarClientMap.containsKey(info.getKey())) {
				return;
			}
			// 使用明确的配置Map替代JSON解析
			Map<String, Object> pulsarConfig = new HashMap<>();
			String servers = SpringUtils.getAppConfig(String.format("magic.api.pulsar.%s.servers",info.getKey()),info.getServers());
			pulsarConfig.put("serviceUrl", "pulsar://" + servers);
			String others = SpringUtils.getAppConfig(String.format("magic.api.pulsar.%s.other",info.getKey()),info.getOther());
			// 添加其他必要配置
			if (StringUtils.isNotBlank(others)) {
				try {
					// 使用Pulsar内部的shaded Jackson解析
					ObjectMapper shadedMapper = new org.apache.pulsar.shade.com.fasterxml.jackson.databind.ObjectMapper();
					Map<String, Object> otherConfig = shadedMapper.readValue(others,
							new org.apache.pulsar.shade.com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
					pulsarConfig.putAll(otherConfig);
				} catch (Exception e) {
					log.error("解析Pulsar配置失败", e);
				}
			}

			// 构建客户端
			this.pulsarClientMap.put(info.getKey(), PulsarClient.builder()
					.loadConf(pulsarConfig)
					.build());
		}
	}

	/**
	 * 注册生产者数据源（可以运行时注册）
	 *
	 * @param info 数据源信息
	 */
	public boolean putProducer(PulsarDataSourceInfo info) {
		try {
			putPulsarClient(info);
			this.producerMap.put(info.getKey(), new PulsarProducer(this, info));
			log.info("注册 Pulsar 生产者:[{}]完成执行", info.getKey());
		} catch (Exception e) {
			log.error("Pulsar 生产者:[{}]注册失败", info.getKey(),e);
			return false;
		}
		return true;
	}

	/**
	 * 注册消费者数据源（可以运行时注册）
	 *
	 * @param info 数据源信息
	 */
	public boolean putConsumer(PulsarDataSourceInfo info, List<PulsarSubscribeScriptInfo> subscribeScripteInfoList) {
		Object lockInfo = null;
		try {
			lockInfo = lock.lock(info.getKey(), 5000, 5000);
			putPulsarClient(info);
			PulsarConsumer pulsarConsumer = this.consumerMap.get(info.getKey());
			if (pulsarConsumer != null) {
				pulsarConsumer.createPulsarConsumer(subscribeScripteInfoList);
			} else {
				pulsarConsumer = new PulsarConsumer(this, info, subscribeScripteInfoList);
				this.consumerMap.put(info.getKey(), pulsarConsumer);
			}
			log.info("注册 Pulsar 消费者:[{}]完成执行", info.getKey());
		} catch (Exception e) {
			log.error("Pulsar 消费者:[{}]注册失败", info.getKey(),e);
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
		if (StringUtils.isNotEmpty(datasourceKey)) {
			PulsarProducer producer = this.producerMap.remove(datasourceKey);
			if (producer != null) {
				producer.close();
			}
		}
	}

	/**
	 * 获取全部生产者数据源
	 */
	public Collection<PulsarProducer> producerNodes() {
		List<PulsarProducer> producers = new ArrayList<>(this.producerMap.values());
		try {
			Producer producer = SpringUtils.getBean(Producer.class);
			if (producer != null) {
				producers.add(new PulsarProducer(this, producer));
			}
		} catch (Exception e) {
		}
		return producers;
	}

	/**
	 * 获取生产者数据源
	 *
	 * @param datasourceKey 数据源Key
	 */
	public PulsarProducer getProducer(String datasourceKey) {
		PulsarProducer dataSourceNode = producerMap.get(datasourceKey);
		Assert.isNotNull(dataSourceNode, String.format("找不到 Pulsar 数据源%s", datasourceKey));
		return dataSourceNode;
	}

	/**
	 * 获取消费者数据源
	 *
	 * @param datasourceKey 数据源Key
	 */
	public PulsarConsumer getConsumer(String datasourceKey) {
		// 根据数据源Key从消费者映射中获取对应的消费者实例
		return this.consumerMap.get(datasourceKey);
	}

	/**
	 * 根据数据源键获取对应的Pulsar客户端实例
	 *
	 * @param datasourceKey 数据源的唯一键，用于区分不同的数据源
	 * @return PulsarClient 实例，如果找不到对应的实例，则返回null
	 */
	public PulsarClient getPulsarClient(String datasourceKey) {
		return this.pulsarClientMap.get(datasourceKey);
	}


	/**
	 * 销毁方法，用于清理资源
	 * <p>
	 * 该方法会依次关闭所有Pulsar生产者、消费者和客户端连接，并清空对应的映射表。
	 * 主要执行以下操作：
	 * 1. 关闭所有Pulsar生产者并清空生产者映射表
	 * 2. 关闭所有Pulsar消费者并清空消费者映射表
	 * 3. 关闭所有Pulsar客户端并清空客户端映射表
	 * </p>
	 *
	 * @throws Exception 如果在关闭过程中发生异常
	 */
	@Override
	public void destroy() throws Exception {
		// 关闭所有生产者并清空映射表
		for (PulsarProducer producer : this.producerMap.values()) {
			producer.close();
		}
		this.producerMap.clear();

		// 关闭所有消费者并清空映射表
		for (PulsarConsumer consumer : this.consumerMap.values()) {
			consumer.close();
		}
		this.consumerMap.clear();

		// 关闭所有Pulsar客户端并清空映射表
		for (PulsarClient client : this.pulsarClientMap.values()) {
			if (null != client && !client.isClosed()) {
				try {
					// 尝试关闭client对象
					client.close();
				} catch (Exception e) {
					// 记录关闭客户端时出现的异常
					log.error("Failed to close Pulsar client", e);
				}
			}
		}
		this.pulsarClientMap.clear();

	}

}
