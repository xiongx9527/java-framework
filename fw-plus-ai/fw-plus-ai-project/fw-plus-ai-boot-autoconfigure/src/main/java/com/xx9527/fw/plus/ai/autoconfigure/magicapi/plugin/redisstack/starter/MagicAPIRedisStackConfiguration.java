package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.redisstack.starter;

import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.redisstack.module.RedisStackModule;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;

@Slf4j
@ConditionalOnProperty(prefix = "magic-api.plugin.redisstack", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.springframework.data.redis.core.StringRedisTemplate","org.apache.commons.pool2.impl.GenericObjectPoolConfig",
        "org.ssssssss.magicapi.redis.RedisModule","dev.langchain4j.store.embedding.redis.RedisEmbeddingStore"})
@Configuration
public class MagicAPIRedisStackConfiguration implements MagicPluginConfiguration {
    /**
     * 返回一个表示RedisStack插件的Plugin对象。
     *
     * @return Plugin对象，其名称为"redisstack"。
     */
    @Override
    public Plugin plugin() {
        return new Plugin("redisstack");
    }

    /**
     * 创建并配置一个RedisStackModule实例。
     *
     * 该方法通过依赖注入获取RedisModule和EmbeddingModel实例，
     * 并将它们传递给RedisStackModule的构造函数以完成模块的初始化。
     *
     * @param redisModule Redis模块实例，用于提供Redis相关功能支持。
     * @param embeddingModel 嵌入模型实例（可选），用于处理向量化操作；如果未提供则为null。
     * @return 返回配置好的RedisStackModule实例。
     */
    @Bean
    public RedisStackModule redisStackModule(RedisModule redisModule, @Autowired(required = false) EmbeddingModel embeddingModel) {
        return new RedisStackModule(redisModule, embeddingModel);
    }
}
