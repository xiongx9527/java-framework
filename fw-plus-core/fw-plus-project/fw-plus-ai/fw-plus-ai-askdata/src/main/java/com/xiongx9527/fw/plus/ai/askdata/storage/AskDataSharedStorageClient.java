package com.xiongx9527.fw.plus.ai.askdata.storage;

/**
 * 共享存储原子操作入口。JDBC/Redis 适配层负责将这些操作映射为其事务、行锁或原子命令，Core 不依赖具体客户端。
 */
public interface AskDataSharedStorageClient {
	Object get(String key);
	boolean putIfAbsent(String key, Object value);
	boolean compareAndSet(String key, Object expectedValue, Object newValue);
	void put(String key, Object value);
}
