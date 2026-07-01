package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.model;


import com.xx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.LocalLockModule;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.Lock;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.consumer.RocketMQConsumer;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.consumer.RocketMqConsumerService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.rocketmq.producer.RocketMqProducer;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.springframework.beans.factory.DisposableBean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RocketMq 动态数据源对象
 *
 * @author tc
 */
@Slf4j
public class MagicDynamicRocketMqDataSource implements DisposableBean {

	/**
	 * RocketMQ生产者实例缓存，Key为数据源标识
	 */
	private final Map<String, RocketMqProducer> producerMap = new ConcurrentHashMap<>();

	/**
	 * RocketMQ消费者实例缓存，Key为消费者组标识
	 */
	private final Map<String, RocketMQConsumer> consumerMap = new ConcurrentHashMap<>();

	/**
	 * RocketMQ管理扩展实例，用于执行运维操作
	 */
	private DefaultMQAdminExt defaultAdminExt;

	/**
	 * RocketMQ管理扩展实例缓存，用于执行运维操作，Key为数据源标识
	 */
	private final Map<String, DefaultMQAdminExt> adminExtMap = new ConcurrentHashMap<>();

	/**
	 * RocketMQ消费者服务，负责消费者的生命周期管理
	 */
	private final RocketMqConsumerService rocketMqConsumerService;

	/**
	 * 分布式锁，用于确保数据源注册操作的并发安全
	 */
	private final Lock lock  = new LocalLockModule();

	/**
	 * 构造方法
	 *
	 * @param rocketMqConsumerService RocketMQ消费者服务
	 */
	public MagicDynamicRocketMqDataSource(RocketMqConsumerService rocketMqConsumerService) {
		this.rocketMqConsumerService = rocketMqConsumerService;
	}

	/**
	 * 创建分布式锁的Key
	 *
	 * @param datasourceKey 数据源Key
	 * @return 锁的Key
	 */
	private String createLockKey(String datasourceKey) {
		return String.format("magic.api.rocketmq.lock.%s", datasourceKey);
	}


	/**
	 * 获取默认的RocketMQ管理端实例
	 *
	 * @return DefaultMQAdminExt实例
	 */
	public DefaultMQAdminExt getDefaultAdminExt() {
		return this.defaultAdminExt;
	}

	/**
	 * 创建并启动默认的RocketMQ管理端实例
	 * 使用双重检查锁定（DCL）确保在并发环境下只初始化一次
	 *
	 * @param nameServer NameServer地址
	 * @throws MQClientException 当客户端操作失败时抛出
	 */
	public void  createDefaultAdminExt(String nameServer) throws MQClientException {
		if (this.defaultAdminExt == null) {
			synchronized (nameServer){
				if (this.defaultAdminExt == null) {
					this.defaultAdminExt = new DefaultMQAdminExt();
					this.defaultAdminExt.setNamesrvAddr(nameServer);
					this.defaultAdminExt.start();
				}
			}
		}
	}

	/**
	 * 注册RocketMQ管理端实例
	 * 使用分布式锁确保并发安全，如果已存在则直接返回。在替换旧实例时会先关闭旧连接以防止资源泄漏。
	 *
	 * @param info RocketMQ数据源配置信息
	 * @return 注册成功返回true，失败返回false
	 */
	public boolean putAdminExt(RocketMqDataSourceInfo info) {
		log.info("开始注册 RocketMq 管理端:[{}]", info.getKey());
		if (this.adminExtMap.containsKey(info.getKey())) {
			return true;
        }
		Object lockInfo = null;
		try {
			// 获取分布式锁，防止并发创建多个实例
			lockInfo = lock.lock(createLockKey(info.getKey()), 5000, 5000);
			if (this.adminExtMap.containsKey(info.getKey())) {
				return true;
			}
			String nameServer = this.producerMap.get(info.getKey()).getProducer().getNamesrvAddr();
			DefaultMQAdminExt adminExt = new DefaultMQAdminExt();
			adminExt.setNamesrvAddr(nameServer);
			adminExt.start();
			DefaultMQAdminExt oldAdminExt = this.adminExtMap.put(info.getKey(), adminExt);
			if (oldAdminExt != null) {
				try {
					oldAdminExt.shutdown();
				} catch (Exception e) {
					log.error("RocketMq 管理端:[{}]关闭失败", info.getKey());
				}
			}
			log.info("注册 RocketMq 管理端:[{}]完成执行", info.getKey());
		} catch (Exception e) {
			log.error("RocketMq 管理端:[{}]注册失败", info.getKey());
			return false;
		} finally {
			// 释放分布式锁
			if (lockInfo != null) {
				lock.release(lockInfo);
			}
		}
		return true;
	}

	/**
	 * 注册生产者数据源（可以运行时注册）
	 *
	 * @param info 数据源信息
	 */
	public boolean putProducer(RocketMqDataSourceInfo info) {
		Object lockInfo = null;
		try {
			lockInfo = lock.lock(createLockKey(info.getKey()), 5000, 5000);
			RocketMqProducer producer = this.producerMap.put(info.getKey(), new RocketMqProducer(info));
			if (producer != null) {
				producer.close();
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

			DefaultMQAdminExt adminExt = this.adminExtMap.remove(datasourceKey);
			if (adminExt != null) {
				try {
					adminExt.shutdown();
				} catch (Exception e) {
					log.error("RocketMq 消息队列:[{}]关闭失败", datasourceKey);
				}
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
	 * 获取消费者实例
	 *
	 * @param datasourceKey 数据源Key
	 * @return RocketMQ消费者实例，如果不存在则返回null
	 */
	public RocketMQConsumer getConsumer(String datasourceKey) {
		return this.consumerMap.get(datasourceKey);
	}

	/**
	 * 获取生产者实例
	 *
	 * @param datasourceKey 数据源Key
	 * @return RocketMQ生产者实例，如果不存在则返回null
	 */
	public RocketMqProducer getProducer(String datasourceKey) {
		return this.producerMap.get(datasourceKey);
	}
	
	/**
	 * 获取管理端扩展实例
	 *
	 * @param datasourceKey 数据源Key
	 * @return DefaultMQAdminExt实例，如果不存在则返回null
	 */
	public DefaultMQAdminExt getAdminExt(String datasourceKey) {
		return this.adminExtMap.get(datasourceKey);
	}

	/**
	 * 销毁数据源，关闭所有RocketMQ生产者、消费者和管理端连接
	 * 该方法会在应用 shutdown 时自动调用，确保资源正确释放。
	 * 即使某个实例关闭失败，也会继续尝试关闭其他实例，防止资源泄漏。
	 *
	 * @throws Exception 当关闭连接过程中发生异常时抛出
	 */
	@Override
	public void destroy() throws Exception {
		log.info("开始关闭所有 RocketMQ 生产者、消费者和管理端连接...");
		
		// 关闭所有生产者
		this.producerMap.forEach((key, producer) -> {
			try {
				if (producer != null) {
					producer.close();
					log.debug("已关闭 RocketMQ 生产者: {}", key);
				}
			} catch (Exception e) {
				log.error("关闭 RocketMQ 生产者 {} 时发生异常", key, e);
			}
		});
		this.producerMap.clear();
		
		// 关闭所有消费者
		this.consumerMap.forEach((key, consumer) -> {
			try {
				if (consumer != null) {
					consumer.close();
					log.debug("已关闭 RocketMQ 消费者: {}", key);
				}
			} catch (Exception e) {
				log.error("关闭 RocketMQ 消费者 {} 时发生异常", key, e);
			}
		});
		this.consumerMap.clear();
		
		// 关闭所有管理端扩展
		this.adminExtMap.forEach((key, adminExt) -> {
			try {
				if (adminExt != null) {
					adminExt.shutdown();
					log.debug("已关闭 RocketMQ 管理端: {}", key);
				}
			} catch (Exception e) {
				log.error("关闭 RocketMQ 管理端 {} 时发生异常", key, e);
			}
		});
		this.adminExtMap.clear();

		if (null != defaultAdminExt) {
			try {
				defaultAdminExt.shutdown();
			} catch (Exception e) {
				log.error("关闭 RocketMQ 管理端 {} 时发生异常", defaultAdminExt, e);
			}
		}
		
		log.info("所有 RocketMQ 连接已关闭");
	}

}
