package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.redisstack.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.exception.MagicAPIVectorException;
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
public class RedisStackModule implements DynamicAttribute<RedisStackModule, VectorStoreOperations>, DynamicModule<VectorStoreOperations> {

    public final RedisModule redisModule;
    private final LegacyVectorModuleSupport legacySupport;
    private final VectorStoreOperations operations;

    @Deprecated
    public RedisStackModule(RedisModule redisModule, EmbeddingModel embeddingModel) {
        this(redisModule, (LegacyVectorModuleSupport) null, null);
    }

    public RedisStackModule(RedisModule redisModule, EmbeddingModel embeddingModel, EmbeddingStoreRegistry registry) {
        this(redisModule, new LegacyVectorModuleSupport(registry, "redisstack"), null);
    }

    private RedisStackModule(RedisModule redisModule, LegacyVectorModuleSupport legacySupport,
                             VectorStoreOperations operations) {
        this.redisModule = redisModule;
        this.legacySupport = legacySupport;
        this.operations = operations;
    }

    public VectorStoreOperations getOperations() {
        return operations;
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String indexName, Integer dimension, Collection<String> metadataKeys) {
        return requireSupport().get(indexName).getEmbeddingStore();
    }

    @Deprecated
    public EmbeddingStore buildEmbeddingStore(String indexName, Integer dimension) {
        return requireSupport().get(indexName).getEmbeddingStore();
    }

    @Override
    public RedisStackModule getDynamicAttribute(String key) {
        return new RedisStackModule(redisModule, requireSupport(), requireSupport().get(key));
    }

    @Override
    public VectorStoreOperations getDynamicModule(MagicScriptContext context) {
        return requireSupport().getDefault();
    }

    private LegacyVectorModuleSupport requireSupport() {
        if (legacySupport == null) {
            throw new MagicAPIVectorException("Legacy Magic-API vector provider 'redisstack' is deprecated; migrate script to import vector and access vector['default']. Missing key: default", null);
        }
        return legacySupport;
    }
}
