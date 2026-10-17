package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.broadcast;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;

/**
 * 本地广播投递线程池。
 *
 * <p>同步投递时，一个慢客户端会阻塞整条广播链路；Redis 监听器触发的广播还会因此阻塞
 * Redis 监听线程。开启异步后投递提交到有界线程池，队列满时丢弃并告警，
 * 以"宁可丢一条广播"换取"不让个别慢连接拖垮整条链路"。</p>
 *
 * @author zzy
 */
@Slf4j
public class WsBroadcastExecutor {

	private final ThreadPoolTaskExecutor executor;

	public WsBroadcastExecutor(int coreSize, int maxSize, int queueCapacity) {
		this.executor = new ThreadPoolTaskExecutor();
		this.executor.setCorePoolSize(coreSize);
		this.executor.setMaxPoolSize(maxSize);
		this.executor.setQueueCapacity(queueCapacity);
		this.executor.setThreadNamePrefix("ws-broadcast-");
		this.executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
		this.executor.initialize();
	}

	/**
	 * 提交一次广播投递；线程池与队列均已满时丢弃并记录告警。
	 *
	 * @param task 投递任务
	 * @return true 表示已接受
	 */
	public boolean submit(Runnable task) {
		try {
			executor.execute(task);
			return true;
		} catch (RejectedExecutionException e) {
			log.warn("WebSocket broadcast queue is full, dropping broadcast task");
			return false;
		}
	}

	/**
	 * 关闭线程池。
	 */
	public void shutdown() {
		executor.shutdown();
	}

	/**
	 * 提交并保留句柄，供需要等待结果的场景使用。
	 *
	 * @param task 投递任务
	 * @return 异步句柄，队列满时返回 null
	 */
	public CompletableFuture<?> submitCompletable(Runnable task) {
		try {
			return executor.submitCompletable(task);
		} catch (RejectedExecutionException e) {
			log.warn("WebSocket broadcast queue is full, dropping broadcast task");
			return null;
		}
	}
}
