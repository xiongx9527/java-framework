package com.xiongx9527.fw.plus.ai.askdata.storage;

/**
 * Redis 共享存储适配入口，不绑定 Lettuce、Jedis 或 Spring Data Redis。
 *
 * <p>{@code putIfAbsent} 应映射为 SET NX；{@code compareAndSet} 应使用 Lua 或 WATCH/MULTI 保证比较、更新和删除原子性。
 * 不允许由客户端在应用线程中执行非原子的 GET 后 SET。</p>
 */
public interface AskDataRedisStorageClient extends AskDataSharedStorageClient {
}
