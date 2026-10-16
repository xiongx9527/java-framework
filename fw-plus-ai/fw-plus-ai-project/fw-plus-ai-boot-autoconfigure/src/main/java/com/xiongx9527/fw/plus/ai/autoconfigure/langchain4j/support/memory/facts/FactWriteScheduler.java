package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 事实写入异步调度器（JVM 静态共享）：daemon 单线程 + 有界队列（容量 {@value #QUEUE_CAPACITY}）。
 *
 * <p>抽取是「尽力而为」的后台任务：队列满即拒绝（{@link #submit} 返回 {@code false}，
 * 由调用方 warn + 丢弃），绝不反压主对话；任务异常在调用方的 Runnable 内被隔离，
 * 不会拖垮调度线程。</p>
 */
public final class FactWriteScheduler {

	/** 有界队列容量（满则拒绝） */
	public static final int QUEUE_CAPACITY = 128;

	private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
			new ArrayBlockingQueue<Runnable>(QUEUE_CAPACITY), new FactThreadFactory());

	private FactWriteScheduler() {
	}

	/** 提交后台任务；队列满（拒绝）返回 {@code false}，由调用方 warn + 丢弃。 */
	public static boolean submit(Runnable task) {
		if (task == null) {
			return false;
		}
		try {
			EXECUTOR.execute(task);
			return true;
		} catch (RejectedExecutionException e) {
			return false;
		}
	}

	/** 当前排队任务数（诊断用） */
	public static int queueSize() {
		return EXECUTOR.getQueue().size();
	}

	private static final class FactThreadFactory implements ThreadFactory {

		private final AtomicInteger seq = new AtomicInteger();

		@Override
		public Thread newThread(Runnable runnable) {
			Thread thread = new Thread(runnable, "fact-write-" + seq.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		}
	}
}
