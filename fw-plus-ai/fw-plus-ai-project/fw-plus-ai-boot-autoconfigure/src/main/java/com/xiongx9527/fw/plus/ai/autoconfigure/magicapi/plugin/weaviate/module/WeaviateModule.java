package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.model.MagicDynamicWeaviateDataSource;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.v1.auth.provider.AccessTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

@Slf4j
@MagicModule("weaviate")
public class WeaviateModule implements DynamicAttribute<WeaviateModule, WeaviateModule>, DynamicModule<WeaviateModule> {

    /**
     * 配置字段，用于存储配置信息的反射字段对象
     * 该字段被声明为final static，确保在类加载时初始化且不可更改
     */
    private final static Field configField;

    /**
     * 访问令牌提供者字段，用于存储访问令牌提供者实例的反射字段对象
     * 该字段被声明为final static，确保在类加载时初始化且不可更改
     */
    private final static Field accessTokenProviderField;

    static {
        // 通过反射获取WeaviateClient类中的config字段
        try {
            configField = WeaviateClient.class.getDeclaredField("config");
            // 设置字段可访问，绕过Java访问控制检查
            configField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // 当目标字段不存在时抛出运行时异常
            throw new RuntimeException("Failed to access connectConfig field: field not found", e);
        } catch (SecurityException e) {
            // 当安全管理器阻止访问时抛出运行时异常
            throw new RuntimeException("Failed to access connectConfig field: access denied by security manager", e);
        }

        // 通过反射获取WeaviateClient类中的tokenProvider字段
        try {
            accessTokenProviderField = WeaviateClient.class.getDeclaredField("tokenProvider");
            // 设置字段可访问，绕过Java访问控制检查
            accessTokenProviderField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            // 当目标字段不存在时抛出运行时异常
            throw new RuntimeException("Failed to access connectConfig field: field not found", e);
        } catch (SecurityException e) {
            // 当安全管理器阻止访问时抛出运行时异常
            throw new RuntimeException("Failed to access connectConfig field: access denied by security manager", e);
        }
    }


    /** 动态Weaviate数据源 */
    private final MagicDynamicWeaviateDataSource magicDynamicWeaviateDataSource;

    /**
     * Weaviate客户端实例，用于与Weaviate向量数据库进行交互
     */
    private final WeaviateClient weaviateClient;

        /**
     * 嵌入模型实例，用于生成文本的向量表示
     *
     * 该字段存储了用于文本嵌入的模型对象，通常用于将文本转换为数值向量
     * 以便进行相似度计算、聚类等机器学习操作
     */
    private final EmbeddingModel embeddingModel;


    /**
     * 构造函数，初始化WeaviateModule实例
     *
     * @param magicDynamicWeaviateDataSource 用于动态数据源操作的MagicDynamicWeaviateDataSource实例
     * @param weaviateClient Weaviate客户端实例，用于与Weaviate服务进行交互
     * @param embeddingModel 嵌入模型实例，用于处理向量嵌入相关操作
     */
    public WeaviateModule(MagicDynamicWeaviateDataSource magicDynamicWeaviateDataSource, WeaviateClient weaviateClient, EmbeddingModel embeddingModel) {
        this.magicDynamicWeaviateDataSource = magicDynamicWeaviateDataSource;
        this.weaviateClient = weaviateClient;
        this.embeddingModel = embeddingModel;
    }


    /**
     * 获取配置对象
     *
     * @return Config 配置对象
     * @throws RuntimeException 当无法访问configField字段时抛出
     */
    private Config getConfig() {
        try {
            // 通过反射获取weaviateClient对象中的configField字段值
            return (Config) configField.get(this.weaviateClient);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取访问令牌提供者
     *
     * @return AccessTokenProvider 访问令牌提供者对象
     * @throws RuntimeException 当无法访问accessTokenProviderField字段时抛出
     */
    private AccessTokenProvider getAccessTokenProvider() {
        try {
            // 通过反射获取weaviateClient对象中的accessTokenProviderField字段值
            return (AccessTokenProvider) accessTokenProviderField.get(this.weaviateClient);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取Weaviate客户端实例
     *
     * @return WeaviateClient 返回当前对象持有的weaviate客户端实例
     */
    public WeaviateClient getClient() {
        return this.weaviateClient;
    }

    /**
     * 构建并返回一个Weaviate嵌入存储实例
     *
     * @param objectClass 对象类名，用于指定Weaviate中的数据类别
     * @param avoidDups 是否避免重复数据的插入
     * @param consistencyLevel 数据一致性级别设置
     * @param metadataKeys 元数据键集合，用于指定需要存储的元数据字段
     * @param textFieldName 文本字段名称，用于指定存储文本内容的字段名
     * @return 返回配置好的EmbeddingStore实例
     */
    public EmbeddingStore buildEmbeddingStore(String objectClass, Boolean avoidDups, String consistencyLevel, Collection<String> metadataKeys, String textFieldName) {
        // 获取配置信息并解析基础URL
        Config config = getConfig();
        String baseURLStr = config.getBaseURL();
        if (StringUtils.isBlank(baseURLStr)) {
            throw new IllegalArgumentException("Base URL must not be null or blank");
        }

        URI baseUrl;
        try {
            baseUrl = new URI(baseURLStr);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid base URL: " + baseURLStr, e);
        }

        String scheme = baseUrl.getScheme();
        String host = baseUrl.getHost();
        Integer port = baseUrl.getPort();

        // 处理gRPC相关配置
        boolean useGrpcForInserts = Boolean.TRUE.equals(config.useGRPC());
        boolean securedGrpc = Boolean.TRUE.equals(config.isGRPCSecured());
        Integer grpcPort = null;

        String grpcHostStr = config.getGRPCHost();
        if (StringUtils.isNotBlank(grpcHostStr)) {
            URI grpcUrl;
            try {
                // 注意：这里假设 getGRPCHost() 不包含 scheme，否则应做进一步解析
                grpcUrl = new URI(scheme + "://" + grpcHostStr);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid gRPC host: " + grpcHostStr, e);
            }
            grpcPort = grpcUrl.getPort();
        }

        // 获取访问令牌作为API密钥
        String apiKey = "";
        AccessTokenProvider accessTokenProvider = getAccessTokenProvider();
        if (accessTokenProvider != null) {
            String token = accessTokenProvider.getAccessToken();
            if (token != null) {
                apiKey = token;
            }
        }

        // 创建并返回Weaviate嵌入存储实例
        WeaviateEmbeddingStore weaviateEmbeddingStore = new WeaviateEmbeddingStore(
                apiKey,
                scheme, host, port,
                useGrpcForInserts, securedGrpc, grpcPort,
                objectClass, avoidDups, consistencyLevel, metadataKeys, textFieldName);
        return weaviateEmbeddingStore;
    }

    /**
     * 构建嵌入存储实例
     *
     * @param objectClass 对象类名称，用于标识存储的数据类型
     * @param metadataKeys 元数据键集合，指定需要存储的元数据字段
     * @return 返回配置好的EmbeddingStore实例
     */
    public EmbeddingStore buildEmbeddingStore(String objectClass,Collection<String> metadataKeys) {
        return buildEmbeddingStore(objectClass, null, null, metadataKeys, null);
    }

    /**
     * 构建嵌入存储实例
     * 该方法是buildEmbeddingStore的重载方法，用于创建指定对象类型的嵌入存储，使用默认的配置参数
     *
     * @param objectClass 对象类型标识符，用于确定要构建的嵌入存储的具体类型
     * @return 返回构建好的EmbeddingStore实例
     */
    public EmbeddingStore buildEmbeddingStore(String objectClass) {
        return buildEmbeddingStore(objectClass, null);
    }

    /**
     * 获取动态模块实例
     *
     * @param context 魔法脚本上下文，用于获取数据源配置信息
     * @return 返回WeaviateModule实例，如果数据源键为空则返回当前实例，否则返回新的WeaviateModule实例
     */
    @Override
    public WeaviateModule getDynamicModule(MagicScriptContext context) {
        // 从上下文中获取数据源键
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        // 如果数据源键为空，直接返回当前模块实例
        if (StringUtils.isEmpty(dataSourceKey)) return this;
        // 根据数据源键创建新的WeaviateModule实例
        return new WeaviateModule(this.magicDynamicWeaviateDataSource, this.magicDynamicWeaviateDataSource.getClient(dataSourceKey), this.embeddingModel);
    }

    /**
     * 获取动态属性对应的Weaviate模块实例
     *
     * @param key 动态数据源的键值，用于获取对应的客户端实例
     * @return 返回一个新的WeaviateModule实例，包含动态数据源、对应key的客户端以及嵌入模型
     */
    @Override
    public WeaviateModule getDynamicAttribute(String key) {
        // 创建并返回新的WeaviateModule实例，使用当前数据源、指定key的客户端以及嵌入模型进行初始化
        return new WeaviateModule(this.magicDynamicWeaviateDataSource, this.magicDynamicWeaviateDataSource.getClient(key), this.embeddingModel);
    }

}
