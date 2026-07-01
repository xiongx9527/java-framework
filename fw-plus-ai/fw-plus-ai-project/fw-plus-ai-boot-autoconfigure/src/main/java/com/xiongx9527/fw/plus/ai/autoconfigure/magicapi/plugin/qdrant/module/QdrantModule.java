package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.model.MagicDynamicQdrantDataSource;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore;
import io.qdrant.client.QdrantClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@Slf4j
@MagicModule("qdrant")
public class QdrantModule implements DynamicAttribute<QdrantModule, QdrantModule>, DynamicModule<QdrantModule> {

    /** 动态Qdrant数据源 */
    private final MagicDynamicQdrantDataSource magicDynamicQdrantDataSource;

    /** Qdrant客户端 */
    private final QdrantClient qdrantClient;

    /**
     * 嵌入模型实例，用于生成文本的向量表示
     *
     * 该字段存储了用于文本嵌入的模型对象，通常用于将文本转换为数值向量
     * 以便进行相似度计算、聚类等机器学习操作
     */
    private final EmbeddingModel embeddingModel;

    /**
     * QdrantModule构造函数
     *
     * @param magicDynamicQdrantDataSource 动态数据源配置对象，用于管理Qdrant数据源连接
     * @param qdrantClient Qdrant客户端实例，用于与Qdrant向量数据库进行交互
     * @param embeddingModel 嵌入模型实例，用于文本向量化处理
     */
    public QdrantModule(MagicDynamicQdrantDataSource magicDynamicQdrantDataSource, QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        this.magicDynamicQdrantDataSource = magicDynamicQdrantDataSource;
        this.qdrantClient = qdrantClient;
        this.embeddingModel = embeddingModel;
    }

    /**
     * 获取Qdrant客户端
     * 
     * @return Qdrant客户端实例
     */
    public QdrantClient getClient() {
        return this.qdrantClient;
    }

    /**
     * 构建嵌入存储实例
     *
     * @param collectionName 集合名称，用于指定Qdrant中的集合
     * @param payloadTextKey 负载文本键，用于指定存储文本内容的字段名，可为空
     * @return 返回构建好的嵌入存储实例
     */
    public EmbeddingStore buildEmbeddingStore(String collectionName,String payloadTextKey) {
        // 创建Qdrant嵌入存储构建器，设置客户端和集合名称
        QdrantEmbeddingStore.Builder builder = QdrantEmbeddingStore.builder().client(this.qdrantClient).collectionName(collectionName);
        // 如果负载文本键不为空，则设置负载文本键
        if (StringUtils.isNotBlank(payloadTextKey)) {
            builder.payloadTextKey(payloadTextKey);
        }
        // 构建并返回嵌入存储实例
        return builder.build();
    }


    /**
     * 获取动态模块
     * 
     * @param context 脚本上下文
     * @return Qdrant模块实例
     */
    @Override
    public QdrantModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) return this;
        return new QdrantModule(this.magicDynamicQdrantDataSource, this.magicDynamicQdrantDataSource.getClient(dataSourceKey), this.embeddingModel);
    }

    /**
     * 获取动态属性
     * 
     * @param key 数据源键
     * @return Qdrant模块实例
     */
    @Override
    public QdrantModule getDynamicAttribute(String key) {
        return new QdrantModule(this.magicDynamicQdrantDataSource, this.magicDynamicQdrantDataSource.getClient(key), this.embeddingModel);
    }

}
