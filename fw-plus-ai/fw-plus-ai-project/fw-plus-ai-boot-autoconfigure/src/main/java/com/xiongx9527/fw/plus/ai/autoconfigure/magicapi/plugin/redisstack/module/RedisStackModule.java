package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.redisstack.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.legacy.LegacyVectorModuleSupport;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.module.VectorStoreOperations;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.module.RedisModule;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.Collection;

@Deprecated
public class RedisStackModule implements DynamicAttribute<VectorStoreOperations, VectorStoreOperations>, DynamicModule<VectorStoreOperations> {

    public final RedisModule redisModule;
    private final LegacyVectorModuleSupport legacySupport;

    @Deprecated
    public RedisStackModule(RedisModule redisModule, EmbeddingModel embeddingModel) {
        this(redisModule, new LegacyVectorModuleSupport(null, "redisstack"));
    }

    public RedisStackModule(RedisModule redisModule, EmbeddingModel embeddingModel, EmbeddingStoreRegistry registry) {
        this(redisModule, new LegacyVectorModuleSupport(registry, "redisstack"));
    }

    private RedisStackModule(RedisModule redisModule, LegacyVectorModuleSupport legacySupport) {
        this.redisModule = redisModule;
        this.legacySupport = legacySupport;
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String indexName, Integer dimension, Collection<String> metadataKeys) {
        throw LegacyVectorModuleSupport.unsupportedBuilder("redisstack");
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String indexName, Integer dimension) {
        throw LegacyVectorModuleSupport.unsupportedBuilder("redisstack");
    }

    @Override
    public VectorStoreOperations getDynamicAttribute(String key) {
        return legacySupport.get(key);
    }

    @Override
    public VectorStoreOperations getDynamicModule(MagicScriptContext context) {
        return legacySupport.getDefault();
    }
}
