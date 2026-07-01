package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.redisstack.module;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.redis.RedisEmbeddingStore;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.Collection;

public class RedisStackModule implements DynamicAttribute<RedisStackModule, RedisStackModule>, DynamicModule<RedisStackModule> {

    /**
     * RedisModule 实例，用于与 Redis 进行交互。
     * 该字段被声明为 final，确保其在初始化后不可更改。
     */
    public final RedisModule redisModule;

    /**
     * EmbeddingModel 实例，用于处理嵌入模型相关的操作。
     * 该字段被声明为 final，确保其在初始化后不可更改。
     */
    private final EmbeddingModel embeddingModel;

    /**
     * 构造函数，初始化 RedisStackModule 实例。
     *
     * @param redisModule   Redis 模块实例，用于处理 Redis 相关操作。
     * @param embeddingModel 嵌入模型实例，用于处理嵌入相关的逻辑。
     */
    public RedisStackModule(RedisModule redisModule, EmbeddingModel embeddingModel) {
        this.redisModule = redisModule;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 根据 MagicScript 上下文获取动态模块。
     * 如果上下文中未指定数据源键，则返回当前实例；
     * 否则基于指定的数据源键创建新的 RedisStackModule 实例。
     *
     * @param context MagicScript 上下文，包含运行时配置信息。
     * @return 返回当前实例或基于新数据源键创建的 RedisStackModule 实例。
     */
    @Override
    public RedisStackModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) return this;
        return new RedisStackModule(this.redisModule.getDynamicAttribute(dataSourceKey), embeddingModel);
    }

    /**
     * 根据指定键获取动态属性，并返回新的 RedisStackModule 实例。
     *
     * @param key 用于获取动态属性的键。
     * @return 基于指定键创建的新 RedisStackModule 实例。
     */
    @Override
    public RedisStackModule getDynamicAttribute(String key) {
        return new RedisStackModule(this.redisModule.getDynamicAttribute(key), embeddingModel);
    }

    /**
     * 构建并返回一个Redis嵌入存储实例。
     *
     * @param indexName     嵌入存储的索引名称，用于标识存储空间。
     * @param dimension     嵌入向量的维度，表示每个嵌入向量的长度。
     * @param metadataKeys  元数据键集合，用于定义嵌入向量关联的元数据字段。
     * @return              返回配置好的Redis嵌入存储实例。
     */
    public EmbeddingStore buildEmbeddingStore(String indexName, Integer dimension, Collection<String> metadataKeys) {
        // 从redisModule中获取Redis连接所需的主机、端口、用户名和密码信息
        String host = this.redisModule.getHost();
        Integer port = Integer.parseInt(this.redisModule.getPort());
        String username = this.redisModule.getUsername();
        String password = this.redisModule.getPassword();

        // 使用RedisEmbeddingStore.Builder构建嵌入存储实例，并设置相关配置参数
        return RedisEmbeddingStore.builder()
                .host(host)
                .port(port)
                .user(username)
                .password(password)
                .indexName(indexName)
                .dimension(dimension)
                .metadataKeys(metadataKeys)
                .build();
    }

    /**
     * 构建一个嵌入存储实例。
     *
     * @param indexName  嵌入存储的索引名称，用于标识存储的唯一性。
     * @param dimension  嵌入向量的维度，表示每个嵌入向量的长度。
     * @return 返回构建好的嵌入存储实例。
     */
    public EmbeddingStore buildEmbeddingStore(String indexName, Integer dimension) {
        return buildEmbeddingStore(indexName, dimension, null);
    }

}
