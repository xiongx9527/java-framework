
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.task.service;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.task.model.TaskInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ClusterTaskMagicDynamicRegistry extends BaseTaskMagicDynamicRegistry implements ApplicationContextAware  {

	/**
	 * Redis 操作模板，用于执行 Redis 相关操作
	 * 该模板是必须的依赖项，如果无法注入将会导致应用启动失败
	 */
	private StringRedisTemplate stringRedisTemplate;

	/**
	 * 节点ID，用于在集群环境中标识当前节点
	 * 通过UUID生成唯一的节点标识符
	 */
	private final String nodeId = UUID.randomUUID().toString();

	/**
	 * 构造函数，初始化ClusterTaskMagicDynamicRegistry实例
	 *
	 * @param magicResourceStorage 任务信息存储器，用于存储和管理任务信息
	 * @param taskScheduler        任务调度器，用于调度和执行任务
	 * @param showLog              是否显示日志，true表示显示日志，false表示不显示日志
	 */
	public ClusterTaskMagicDynamicRegistry(MagicResourceStorage<TaskInfo> magicResourceStorage, TaskScheduler taskScheduler, boolean showLog) {
		super(magicResourceStorage, taskScheduler, showLog);
	}

	/**
	 * 执行定时任务，在集群环境中通过分布式锁确保同一时间只有一个节点执行任务
	 *
	 * @param mappingNode 任务映射节点，包含任务信息
	 * @param scriptName  脚本名称
	 */
	@Override
	protected void executeTask(MappingNode<TaskInfo> mappingNode, String scriptName) {
		TaskInfo entity = mappingNode.getEntity();
		// 尝试获取分布式锁
		String lockKey = "scheduled_task_lock:" + entity.getId();
		String lockValue = nodeId;
		try {
			// 在尝试获取锁之前添加随机等待
			long randomWait = ThreadLocalRandom.current().nextLong(100); // 0-100毫秒随机等待
			Thread.sleep(randomWait);
		} catch (InterruptedException e) {
			// 恢复中断状态
			Thread.currentThread().interrupt();
			log.warn("定时任务:[{}]在等待执行时被中断，节点:{}", scriptName, nodeId);
		}
		// 锁定30秒，应该足够执行任务
		Boolean lockAcquired = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 30, TimeUnit.SECONDS);

		if (lockAcquired != null && lockAcquired) {
			try {
				if (showLog) {
					log.info("定时任务:[{}]开始执行，节点:{}", scriptName, nodeId);
				}
				executeScript(mappingNode, scriptName);
			} catch (Exception e) {
				log.error("定时任务执行出错", e);
			} finally {
				// 释放锁（不使用Lua脚本）
				releaseLock(lockKey, lockValue);

				if (showLog) {
					log.info("定时任务:[{}]执行完毕，节点:{}", scriptName, nodeId);
				}
			}
		} else {
			if (showLog) {
				log.info("定时任务:[{}]已在其他节点执行，当前节点:{}跳过执行", scriptName, nodeId);
			}
		}
	}

	/**
	 * 释放分布式锁（不使用Lua脚本）
	 *
	 * @param lockKey 锁的键
	 * @param lockValue 锁的值
	 */
	private void releaseLock(String lockKey, String lockValue) {
		try {
			// 先获取锁的值进行比较
			String currentValue = stringRedisTemplate.opsForValue().get(lockKey);
			// 如果值匹配，则删除锁
			if (lockValue.equals(currentValue)) {
				stringRedisTemplate.delete(lockKey);
			}
		} catch (Exception e) {
			log.warn("释放分布式锁失败，key: {}", lockKey, e);
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (this.stringRedisTemplate != null) {
			return ;
		}
		// 从应用上下文中获取所有RedisTemplate类型的Bean实例
		Map<String, StringRedisTemplate> map = MapUtils.emptyIfNull(applicationContext.getBeansOfType(StringRedisTemplate.class));
		for (Map.Entry<String, StringRedisTemplate> entry : map.entrySet()) {
			// 跳过StringRedisTemplate类型的实例，只获取RedisTemplate实例
			if (entry.getValue() instanceof StringRedisTemplate) {
				this.stringRedisTemplate = entry.getValue();
				break;
			}
		}
		// 如果未找到合适的RedisTemplate实例，则抛出运行时异常
		if (this.stringRedisTemplate == null) {
			throw new RuntimeException(I18nMessageResolver.resolveKey("fw.boot.task.redisTemplateNotFound"));
		}
	}
}
