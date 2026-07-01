package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.module;

import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.model.MagicDynamicMilvusDataSource;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.lang.reflect.Field;

@Slf4j
@MagicModule("milvus")
public class MilvusModule  implements DynamicAttribute<MilvusModule, MilvusModule>, DynamicModule<MilvusModule> {

    /**
     * 静态字段，用于存储MilvusClientV2类中connectConfig字段的反射访问对象
     * 通过反射获取MilvusClientV2类的私有字段connectConfig，以便后续能够访问该字段的值
     *
     * 注意：此字段在类加载时初始化，如果无法访问目标字段则会抛出RuntimeException
     */
    private final static Field connectConfigField;

    // 静态初始化块，用于在类加载时获取并设置connectConfig字段的可访问性
    static {
        try {
            // 通过反射获取MilvusClientV2类中的connectConfig私有字段
            connectConfigField = MilvusClientV2.class.getDeclaredField("connectConfig");
            // 设置字段可访问，绕过Java访问控制检查
            connectConfigField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // 当目标字段不存在时抛出运行时异常
            throw new RuntimeException("Failed to access connectConfig field: field not found", e);
        } catch (SecurityException e) {
            // 当安全管理器阻止访问时抛出运行时异常
            throw new RuntimeException("Failed to access connectConfig field: access denied by security manager", e);
        }
    }



    /** 
     * Milvus数据源管理器实例
     * 用于管理和获取Milvus客户端实例
     */
    private final MagicDynamicMilvusDataSource magicDynamicMilvusDataSource;

    /** 
     * Milvus客户端实例
     * 用于执行Milvus相关操作
     */
    private final MilvusClientV2 milvusClientV2;

    /**
     * 嵌入模型实例，用于将文本转换为向量表示
     *
     * 该字段存储了用于生成文本嵌入向量的模型对象，
     * 通常用于语义搜索、文本相似度计算等场景
     */
    private final EmbeddingModel embeddingModel;


    /**
     * 构造函数，初始化Milvus模块
     *
     * @param magicDynamicMilvusDataSource Milvus数据源管理器实例
     * @param milvusClientV2 Milvus客户端实例
     */
    public MilvusModule(MagicDynamicMilvusDataSource magicDynamicMilvusDataSource, MilvusClientV2 milvusClientV2, EmbeddingModel embeddingModel) {
        this.magicDynamicMilvusDataSource = magicDynamicMilvusDataSource;
        this.milvusClientV2 = milvusClientV2;
        this.embeddingModel = embeddingModel;
    }


    /**
     * 获取Milvus客户端实例
     *
     * @return MilvusClientV2 Milvus客户端实例
     */
    public MilvusClientV2 getClient() {
        return this.milvusClientV2;
    }


    /**
     * 获取嵌入模型实例
     *
     * @return 返回当前对象的嵌入模型实例
     */
    public EmbeddingModel getEmbeddingModel() {
        return this.embeddingModel;
    }

    /**
     * 获取连接配置信息
     *
     * @return ConnectConfig 连接配置对象
     * @throws RuntimeException 当无法访问connectConfig字段时抛出运行时异常
     */
    private ConnectConfig getConnectConfig() {
        try {
            // 通过反射获取milvusClientV2对象中的connectConfig字段值
            return (ConnectConfig) connectConfigField.get(this.milvusClientV2);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to access connectConfig field: field is not accessible", e);
        }
    }

    /**
     * 构建Milvus向量存储实例
     *
     * @param collectionName 集合名称，用于指定Milvus中的数据集合
     * @param dimension 向量维度，指定嵌入向量的维度大小
     * @param indexType 索引类型，用于指定向量搜索的索引结构
     * @param metricType 度量类型，用于指定向量间相似度计算方式
     * @param consistencyLevel 一致性级别，用于指定数据一致性要求
     * @param retrieveEmbeddingsOnSearch 搜索时是否返回嵌入向量，true表示返回向量数据
     * @param autoFlushOnInsert 插入时是否自动刷新，true表示插入数据后自动刷新以确保数据可见
     * @return 返回构建好的EmbeddingStore实例，用于向量存储和检索操作
     */
    public EmbeddingStore buildEmbeddingStore(String collectionName, Integer dimension,
                                              IndexType indexType, MetricType metricType, ConsistencyLevelEnum consistencyLevel,
                                              Boolean retrieveEmbeddingsOnSearch, Boolean autoFlushOnInsert) {
        // 获取连接配置信息
        ConnectConfig connectConfig = getConnectConfig();
        if (connectConfig == null) {
            throw new IllegalStateException("ConnectConfig is null, unable to build MilvusEmbeddingStore.");
        }
        // 构建Milvus向量存储实例
        MilvusEmbeddingStore.Builder builder = MilvusEmbeddingStore.builder().uri(connectConfig.getUri())
                .username(connectConfig.getUsername()).password(connectConfig.getPassword())
                .token(connectConfig.getToken()).databaseName(connectConfig.getDbName())
                .collectionName(collectionName).dimension(dimension).indexType(indexType)
                .metricType(metricType).consistencyLevel(consistencyLevel).retrieveEmbeddingsOnSearch(retrieveEmbeddingsOnSearch)
                .autoFlushOnInsert(autoFlushOnInsert);
        return builder.build();
    }

    /**
     * 构建向量存储实例
     *
     * @param collectionName 集合名称，用于标识向量存储的逻辑分组
     * @param dimension 向量维度，指定向量的维数大小
     * @return 返回构建好的EmbeddingStore实例
     */
    public EmbeddingStore buildEmbeddingStore(String collectionName, Integer dimension) {
        return buildEmbeddingStore(collectionName, dimension, IndexType.IVF_FLAT, MetricType.L2, ConsistencyLevelEnum.STRONG, true, true);
    }

    /**
     * 获取动态Milvus模块实例
     * 根据脚本上下文中的数据源配置获取对应的Milvus客户端，并创建新的Milvus模块实例
     *
     * @param context Magic脚本上下文对象
     * @return MilvusModule Milvus模块实例
     */
    @Override
    public MilvusModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) return this;
        return new MilvusModule(this.magicDynamicMilvusDataSource, this.magicDynamicMilvusDataSource.getClient(dataSourceKey), this.embeddingModel);
    }

    /**
     * 根据指定的key获取动态Milvus模块实例
     * 通过数据源管理器获取对应key的客户端，并创建新的Milvus模块实例
     *
     * @param key 数据源key标识
     * @return MilvusModule Milvus模块实例
     */
    @Override
    public MilvusModule getDynamicAttribute(String key) {
        return new MilvusModule(this.magicDynamicMilvusDataSource, this.magicDynamicMilvusDataSource.getClient(key), this.embeddingModel);
    }

}
