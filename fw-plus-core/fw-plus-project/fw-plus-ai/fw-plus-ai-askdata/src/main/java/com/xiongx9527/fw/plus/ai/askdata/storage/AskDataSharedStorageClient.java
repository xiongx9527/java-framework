package com.xiongx9527.fw.plus.ai.askdata.storage;

/**
 * 共享存储原子操作入口。JDBC/Redis 适配层负责将这些操作映射为其事务、行锁或原子命令，Core 不依赖具体客户端。
 *
 * <p>{@link #putIfAbsent(String, Object)} 必须是单条原子写入；{@link #compareAndSet(String, Object, Object)}
 * 必须把读取到的期望值比较与写入作为一个不可分割操作。传入 {@code newValue == null} 表示原子删除，不能存储空值。
 * 客户端必须能够持久化并恢复本模块的不可变值对象，不得把调用拆成无锁的“先读后写”。</p>
 */
public interface AskDataSharedStorageClient {

	Object get(String key);

	boolean putIfAbsent(String key, Object value);

	/**
	 * 原子比较并更新。{@code expectedValue} 必须是最近一次 {@link #get(String)} 返回的值；{@code newValue} 为空时删除。
	 */
	boolean compareAndSet(String key, Object expectedValue, Object newValue);

	void put(String key, Object value);
}
