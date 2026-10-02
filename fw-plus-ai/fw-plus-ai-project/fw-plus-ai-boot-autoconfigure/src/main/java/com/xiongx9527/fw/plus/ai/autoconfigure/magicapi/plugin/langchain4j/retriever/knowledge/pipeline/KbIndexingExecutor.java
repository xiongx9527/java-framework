package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 知识库异步执行器（设计 §2.3）：显式 JDK8 ThreadPoolExecutor（核心 2、有界队列、CallerRunsPolicy），
 * 同时承载入库即时触发、waiting 轮询兜底与 hit_count 异步累加。
 *
 * <p>进程退出时 {@link #shutdown()} 优雅关闭；知识库整体不装配时本 bean 不创建。</p>
 */
public class KbIndexingExecutor implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(KbIndexingExecutor.class);

	private static final int CORE_SIZE = 2;

	private static final int QUEUE_CAPACITY = 1000;

	private static final long POLL_INTERVAL_SECONDS = 30;

	private final ThreadPoolExecutor worker;

	private final ScheduledExecutorService poller;

	public KbIndexingExecutor() {
		ThreadFactory factory = new ThreadFactory() {
			private final AtomicInteger seq = new AtomicInteger();

			@Override
			public Thread newThread(Runnable r) {
				Thread thread = new Thread(r, "kb-indexing-" + seq.incrementAndGet());
				thread.setDaemon(true);
				return thread;
			}
		};
		this.worker = new ThreadPoolExecutor(CORE_SIZE, CORE_SIZE, 0L, TimeUnit.MILLISECONDS,
				new ArrayBlockingQueue<Runnable>(QUEUE_CAPACITY), factory,
				new ThreadPoolExecutor.CallerRunsPolicy());
		this.poller = Executors.newSingleThreadScheduledExecutor(factory);
	}

	/** 提交索引/检索辅助任务（队列满时 CallerRunsPolicy 回压到调用线程）。 */
	public void execute(Runnable task) {
		worker.execute(task);
	}

	/** 启动 waiting 轮询兜底（入库即时触发失败或多实例错峰时由轮询扫描补偿）。 */
	public void startPolling(final Runnable scanWaiting) {
		poller.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					scanWaiting.run();
				} catch (Throwable e) {
					logger.warn("知识库 waiting 轮询扫描失败: {}", e.getMessage());
				}
			}
		}, POLL_INTERVAL_SECONDS, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
	}

	@Override
	public void close() {
		poller.shutdownNow();
		worker.shutdown();
		try {
			if (!worker.awaitTermination(5, TimeUnit.SECONDS)) {
				worker.shutdownNow();
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			worker.shutdownNow();
		}
	}
}
