package com.xiongx9527.fw.plus.ai.askdata.storage;

/**
 * JDBC 共享存储适配入口，不绑定 DataSource 或 JDBC 实现。
 *
 * <p>{@code putIfAbsent} 应使用唯一键约束；{@code compareAndSet} 应在同一事务中使用版本列、条件 UPDATE/DELETE
 * 或行锁完成。读取后在另一个无锁事务中覆盖不满足该契约。</p>
 */
public interface AskDataJdbcStorageClient extends AskDataSharedStorageClient {
}
