package com.xiongx9527.fw.plus.ai.askdata.storage;

/** Redis 共享存储适配入口，不绑定 Lettuce、Jedis 或 Spring Data Redis。 */
public interface AskDataRedisStorageClient extends AskDataSharedStorageClient {
}
