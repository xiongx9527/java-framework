package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.model;

import com.alibaba.fastjson2.JSON;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.common.BaseService;
import com.xx9527.fw.plus.common.util.SpringUtils;
import io.weaviate.client.Config;
import io.weaviate.client.WeaviateAuthClient;
import io.weaviate.client.WeaviateClient;
import io.weaviate.client.v1.auth.nimbus.AuthType;
import io.weaviate.client.v1.auth.nimbus.NimbusAuth;
import io.weaviate.client.v1.auth.provider.AccessTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MagicDynamicWeaviateDataSource implements BaseService, DisposableBean {

    /**
     * 存储Weaviate客户端的线程安全映射表
     * key为数据源标识，value为对应的Weaviate客户端实例
     */
    private Map<String, WeaviateClient> clientMap = new ConcurrentHashMap<>();

    /**
     * 存储Weaviate数据源信息的线程安全映射表
     * key为数据源标识，value为对应的数据源配置信息
     */
    private Map<String, WeaviateDataSourceInfo> infoMap = new ConcurrentHashMap<>();


    /**
     * 从数据源信息的其他配置中提取请求头信息
     *
     * @param info Weaviate数据源信息对象
     * @return 请求头键值对映射表
     */
    private Map<String, String> extractHeadersFromOtherConfig(WeaviateDataSourceInfo info) {
        Map<String, String> headers = new HashMap<>();
        if (StringUtils.isNotBlank(info.getOther())) {
            try {
                // 假设 other 字段存储的是 JSON 格式的配置
                Map<String, Object> otherConfig = JSON.parseObject(info.getOther());
                Object headersObj = otherConfig.get("headers");
                if (headersObj instanceof Map) {
                    // 如果 headers 是一个映射表，则转换为字符串键值对
                    Map<String, Object> headersMap = (Map<String, Object>) headersObj;
                    for (Map.Entry<String, Object> entry : headersMap.entrySet()) {
                        headers.put(entry.getKey(), entry.getValue().toString());
                    }
                }
            } catch (Exception e) {
                log.warn("解析 headers 配置时发生异常", e);
            }
        }

        return headers;
    }

    /**
     * 从其他配置中提取代理配置信息并设置到Config对象中
     *
     * @param otherConfig 包含代理配置信息的Map对象，应包含proxyHost、proxyPort、proxyScheme三个键
     * @param config 需要设置代理配置的目标Config对象
     */
    private void extractProxyConfigFromOther(Map<String, Object> otherConfig, Config config) {
        // 提取代理配置
        Object proxyHost = otherConfig.get("proxyHost");
        Object proxyPort = otherConfig.get("proxyPort");
        Object proxyScheme = otherConfig.get("proxyScheme");
        if (proxyHost == null || proxyPort == null || proxyScheme == null) {
            return;
        }

        config.setProxy(proxyHost.toString(), Integer.parseInt(proxyPort.toString()), proxyScheme.toString());
    }


    /**
     * 从其他配置中提取gRPC相关配置并设置到Config对象中
     *
     * @param otherConfig 包含其他配置信息的Map对象，可能包含gRPC相关配置
     * @param config 配置对象，用于设置提取到的gRPC配置信息
     */
    private void extractGrpcConfigFromOther(Map<String, Object> otherConfig, Config config) {
        // 提取gRPC配置
        Object gRPCSecured = otherConfig.get("gRPCSecured");
        Object gRPCHost = otherConfig.get("gRPCHost");

        if (gRPCSecured == null && gRPCHost == null) {
            return;
        }

        // 设置gRPC安全配置和主机配置到Config对象
        if (gRPCSecured != null) {
            config.setGRPCSecured(Boolean.parseBoolean(gRPCSecured.toString()));
        }

        if (gRPCHost != null) {
            config.setGRPCHost(gRPCHost.toString());
        }
    }



    /**
     * 从Map中安全获取字符串值
     */
    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 从数据源信息的其他配置中提取认证配置信息
     *
     * @param info Weaviate数据源信息对象
     * @param config Weaviate客户端配置对象
     */
    private AccessTokenProvider extractAuthConfigFromOther(WeaviateDataSourceInfo info, Config config) {
        if (StringUtils.isNotBlank(info.getOther())) {
            try {
                // 解析 other 字段中的 JSON 配置
                Map<String, Object> otherConfig = JSON.parseObject(info.getOther());

                // 提取认证配置
                Object authConfigObj = otherConfig.get("auth");
                if (authConfigObj instanceof Map) {
                    Map<String, Object> authConfig = (Map<String, Object>) authConfigObj;

                    // 获取认证参数
                    String clientSecret = getStringValue(authConfig, "clientSecret");
                    String username = getStringValue(authConfig, "username");
                    String password = getStringValue(authConfig, "password");
                    String authTypeStr = getStringValue(authConfig, "authType");

                    // 获取作用域配置
                    List<String> clientScopes = new ArrayList<>();
                    Object scopesObj = authConfig.get("scopes");
                    if (scopesObj instanceof List) {
                        List<?> scopesList = (List<?>) scopesObj;
                        for (Object scope : scopesList) {
                            clientScopes.add(scope.toString());
                        }
                    } else if (scopesObj instanceof String) {
                        String[] scopesArray = ((String) scopesObj).split(",");
                        clientScopes.addAll(Arrays.asList(scopesArray));
                    }

                    // 确定认证类型
                    AuthType authType = AuthType.CLIENT_CREDENTIALS; // 默认值
                    if (StringUtils.isNotBlank(authTypeStr)) {
                        try {
                            authType = AuthType.valueOf(authTypeStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            log.warn("无效的认证类型: {}, 使用默认值 CLIENT_CREDENTIALS", authTypeStr);
                        }
                    }

                    // 构建 AccessTokenProvider
                    NimbusAuth nimbusAuth = new NimbusAuth();
                    AccessTokenProvider tokenProvider = nimbusAuth.getAccessTokenProvider(
                            config, clientSecret, username, password, clientScopes, authType);

                    return tokenProvider;
                }
            } catch (Exception e) {
                log.warn("解析认证配置时发生异常", e);
            }
        }
        return null;

    }

    /**
     * 构建Weaviate客户端实例
     *
     * @param info Weaviate数据源配置信息，包含连接所需的各种配置参数
     * @return 配置完成的WeaviateClient实例
     * @throws Exception 当客户端创建过程中发生错误时抛出异常
     */
    public WeaviateClient buildClient(WeaviateDataSourceInfo info) {
        // 提取请求头配置
        Map<String, String> headers = extractHeadersFromOtherConfig(info);
        String scheme = info.getScheme();
        scheme = MagicAPIConfigUtils.getWeaviateConfig(info.getKey(), "scheme", scheme);
        String host = info.getHost();
        host = MagicAPIConfigUtils.getWeaviateConfig(info.getKey(), "host", host);
        String connectionTimeoutStr = info.getConnectionTimeout();
        connectionTimeoutStr = MagicAPIConfigUtils.getWeaviateConfig(info.getKey(), "connectionTimeout", connectionTimeoutStr);
        int connectionTimeout = Integer.parseInt(connectionTimeoutStr);
        String connectionRequestTimeoutStr = info.getConnectionRequestTimeout();
        connectionRequestTimeoutStr = MagicAPIConfigUtils.getWeaviateConfig(info.getKey(), "connectionRequestTimeout", connectionRequestTimeoutStr);
        int connectionRequestTimeout = Integer.parseInt(connectionRequestTimeoutStr);
        String socketTimeoutStr = info.getSocketTimeout();
        socketTimeoutStr = MagicAPIConfigUtils.getWeaviateConfig(info.getKey(), "socketTimeout", socketTimeoutStr);
        int socketTimeout = Integer.parseInt(socketTimeoutStr);

        // 创建基础配置对象
        Config config = new Config(scheme, host,headers, connectionTimeout, connectionRequestTimeout, socketTimeout);
        String other = info.getOther();
        other = MagicAPIConfigUtils.getWeaviateConfig(info.getKey(), "other", other);
        if (StringUtils.isNotBlank(other)) {
            try {
                // 解析 other 字段中的 JSON 配置
                Map<String, Object> otherConfig = JSON.parseObject(other);
                // 提取并设置代理配置
                extractProxyConfigFromOther(otherConfig, config);
                // 提取并设置gRPC配置
                extractGrpcConfigFromOther(otherConfig, config);
            } catch (Exception e) {
                log.warn("解析代理配置时发生异常", e);
            }
        }

        try {
            String apiKey = info.getApiKey();
            apiKey = MagicAPIConfigUtils.getWeaviateConfig(info.getKey(), "apiKey", apiKey);
            // 根据是否配置API密钥来选择不同的客户端创建方式
            if (StringUtils.isNotBlank(apiKey)) {
                return WeaviateAuthClient.apiKey(config,apiKey);
            }
            return new WeaviateClient(config,extractAuthConfigFromOther(info, config));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建Weaviate客户端并将其存储到映射中
     *
     * @param info Weaviate数据源信息，包含创建客户端所需的所有配置信息
     */
    public void createClient(WeaviateDataSourceInfo info) {
        // 构建Weaviate客户端实例
        WeaviateClient client = buildClient(info);
        // 将客户端实例按key存储到客户端映射中
        this.clientMap.put(info.getKey(), client);
        // 将数据源信息按key存储到信息映射中
        this.infoMap.put(info.getKey(), info);
    }

    /**
     * 根据指定的键获取Weaviate客户端实例
     *
     * @param key 用于查找客户端实例的键值
     * @return 返回与指定键关联的Weaviate客户端实例，如果未找到则返回null
     */
    public WeaviateClient getClient(String key) {
        return this.clientMap.get(key);
    }

    /**
     * 销毁Weaviate客户端实例
     *
     * @param client 待销毁的Weaviate客户端实例
     */
    public void destroyClient(WeaviateClient client) {
        if (client != null) {
            try {
                // TODO: 待实现客户端关闭逻辑
                client = null;
            } catch (Exception e) {
                log.warn("关闭Weaviate客户端时发生异常", e);
            }
        }
    }

    /**
     * 获取所有数据源节点信息
     *
     * @return 数据源信息列表，包含动态创建的数据源和默认数据源
     */
    public List<WeaviateDataSourceInfo> datasourceNodes() {
        // 获取所有动态创建的数据源信息
        List<WeaviateDataSourceInfo>  dataSourceInfoList = new ArrayList<>(infoMap.values());
        try {
            // 尝试获取默认数据源客户端，如果存在则添加默认数据源信息
            WeaviateClient client = SpringUtils.getBean(WeaviateClient.class);
            if (client != null) {
                WeaviateDataSourceInfo dataSourceInfo = new WeaviateDataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName("默认数据源");
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
        }
        return dataSourceInfoList;
    }

    /**
     * 根据指定的key销毁对应的Weaviate客户端
     *
     * @param key 客户端的唯一标识key，用于定位需要销毁的客户端实例
     */
    public void destroyClientByKey(String  key) {
        // 参数校验，空key直接返回
        if (key == null) {
            return;
        }
        try {
            // 从客户端映射表中获取并移除对应的客户端实例
            WeaviateClient client = this.clientMap.get(key);
            if (client != null) {
                try {
                    // 安全地关闭客户端连接
                    destroyClient(client);
                } catch (Exception e) {
                    log.warn("关闭Weaviate客户端时发生异常，key: {}", key, e);
                }
                // 从映射表中移除客户端引用
                this.clientMap.remove(key);
            }
            // 同时清理相关的配置信息
            this.infoMap.remove(key);
        } catch (Exception e) {
            log.warn("销毁数据源客户端失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 销毁数据源客户端资源
     * <p>
     * 该方法会遍历所有已创建的客户端实例，逐一销毁它们，然后清空客户端映射表。
     * 在销毁过程中如果发生异常，会记录警告日志但不会中断整个销毁流程。
     * </p>
     *
     * @throws Exception 如果在销毁过程中发生无法处理的异常
     */
    @Override
    public void destroy() throws Exception {
        // 遍历并销毁所有客户端实例
        for (String key : new ArrayList<>(this.clientMap.keySet())) {
            try {
                destroyClientByKey(key);
            } catch (Exception e) {
                log.warn("销毁数据源客户端失败: {}", e.getMessage(), e);
            }
        }

        // 清空客户端映射表
        try {
            this.clientMap.clear();
            this.infoMap.clear();
        } catch (Exception e) {
            log.warn("销毁数据源客户端失败: {}", e.getMessage(), e);
        }

    }

}
