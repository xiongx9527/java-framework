package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.DisposableBean;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


@Slf4j
public class MagicDynamicElasticSearchDataSource implements DisposableBean {

    /**
     * Elasticsearch REST客户端缓存映射表
     * 用于存储不同数据源对应的REST客户端实例，避免重复创建连接
     */
    private static final Map<String, RestClient> elasticSearchRestClientMap = new ConcurrentHashMap<>();

    /**
     * Elasticsearch数据源信息缓存映射表
     * 用于存储不同数据源的配置信息，包括连接地址、认证信息等
     */
    private static final Map<String, ElasticSearchDataSourceInfo> elasticSearchDataSourceInfoMap = new ConcurrentHashMap<>();


    /**
     * 从 JSON 配置字符串中设置额外的客户端配置
     *
     * @param builder RestClientBuilder实例，用于构建REST客户端
     * @param otherJson 包含额外配置的JSON格式字符串
     *                  支持的配置项包括：
     *                  - compressionEnabled: 压缩功能开关（布尔值或字符串）
     *                  - metaHeaderEnabled: 元数据头功能开关（布尔值或字符串）
     *                  - strictDeprecationMode: 严格弃用模式开关（布尔值或字符串）
     *                  - defaultHeaders: 默认请求头配置（Map<String, String>格式）
     *                  - requestConfig: 请求配置（包含connectTimeout、socketTimeout、connectionRequestTimeout等）
     */
    private void setAdditionalConfig(RestClientBuilder builder, String otherJson) {
        try {
            Map<String, Object> configMap = JSON.parseObject(otherJson);

            // 处理布尔类型配置
            if (configMap.containsKey("compressionEnabled")) {
                Object compressionEnabled = configMap.get("compressionEnabled");
                if (compressionEnabled instanceof Boolean) {
                    builder.setCompressionEnabled((Boolean) compressionEnabled);
                } else if (compressionEnabled instanceof String) {
                    builder.setCompressionEnabled(Boolean.parseBoolean((String) compressionEnabled));
                }
            }

            if (configMap.containsKey("metaHeaderEnabled")) {
                Object metaHeaderEnabled = configMap.get("metaHeaderEnabled");
                if (metaHeaderEnabled instanceof Boolean) {
                    builder.setMetaHeaderEnabled((Boolean) metaHeaderEnabled);
                } else if (metaHeaderEnabled instanceof String) {
                    builder.setMetaHeaderEnabled(Boolean.parseBoolean((String) metaHeaderEnabled));
                }
            }

            if (configMap.containsKey("strictDeprecationMode")) {
                Object strictDeprecationMode = configMap.get("strictDeprecationMode");
                if (strictDeprecationMode instanceof Boolean) {
                    builder.setStrictDeprecationMode((Boolean) strictDeprecationMode);
                } else if (strictDeprecationMode instanceof String) {
                    builder.setStrictDeprecationMode(Boolean.parseBoolean((String) strictDeprecationMode));
                }
            }

            // 设置默认请求头
            if (configMap.containsKey("defaultHeaders")) {
                Object headersObj = configMap.get("defaultHeaders");
                if (headersObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> headersMap = (Map<String, String>) headersObj;
                    Header[] headers = headersMap.entrySet().stream()
                            .map(entry -> new BasicHeader(entry.getKey(), entry.getValue()))
                            .toArray(Header[]::new);
                    builder.setDefaultHeaders(headers);
                }
            }

            // 设置请求配置回调 - 可以配置超时等参数
            if (configMap.containsKey("requestConfig")) {
                Object requestConfigObj = configMap.get("requestConfig");
                if (requestConfigObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> requestConfigMap = (Map<String, Object>) requestConfigObj;
                    builder.setRequestConfigCallback(requestConfigBuilder -> {
                        if (requestConfigMap.containsKey("connectTimeout")) {
                            Object connectTimeout = requestConfigMap.get("connectTimeout");
                            if (connectTimeout instanceof Integer) {
                                requestConfigBuilder.setConnectTimeout((Integer) connectTimeout);
                            } else if (connectTimeout instanceof String) {
                                requestConfigBuilder.setConnectTimeout(Integer.parseInt((String) connectTimeout));
                            }
                        }
                        if (requestConfigMap.containsKey("socketTimeout")) {
                            Object socketTimeout = requestConfigMap.get("socketTimeout");
                            if (socketTimeout instanceof Integer) {
                                requestConfigBuilder.setSocketTimeout((Integer) socketTimeout);
                            } else if (socketTimeout instanceof String) {
                                requestConfigBuilder.setSocketTimeout(Integer.parseInt((String) socketTimeout));
                            }
                        }
                        if (requestConfigMap.containsKey("connectionRequestTimeout")) {
                            Object connectionRequestTimeout = requestConfigMap.get("connectionRequestTimeout");
                            if (connectionRequestTimeout instanceof Integer) {
                                requestConfigBuilder.setConnectionRequestTimeout((Integer) connectionRequestTimeout);
                            } else if (connectionRequestTimeout instanceof String) {
                                requestConfigBuilder.setConnectionRequestTimeout(Integer.parseInt((String) connectionRequestTimeout));
                            }
                        }
                        return requestConfigBuilder;
                    });
                }
            }

        } catch (Exception e) {
            // 记录日志但不抛出异常，避免影响主要功能
            System.err.println("Failed to parse other config: " + e.getMessage());
        }
    }

    /**
     * 创建ElasticSearch REST客户端
     * 根据提供的数据源信息配置并构建REST客户端，支持基本认证、路径前缀和其他额外配置
     *
     * @param dataSourceInfo ElasticSearch数据源配置信息，包含主机名、端口、协议、认证信息等
     * @return 配置好的RestClient实例
     */
    public RestClient createElasticSearchRestClient(ElasticSearchDataSourceInfo dataSourceInfo) {
        String hostname = MagicAPIConfigUtils.getElasticSearchConfig(dataSourceInfo.getKey(), "hostname", dataSourceInfo.getHostname());
        String port = MagicAPIConfigUtils.getElasticSearchConfig(dataSourceInfo.getKey(), "port", dataSourceInfo.getPort());
        String scheme = MagicAPIConfigUtils.getElasticSearchConfig(dataSourceInfo.getKey(), "scheme", dataSourceInfo.getScheme());
        RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostname, Integer.parseInt(port), scheme));

        String username = dataSourceInfo.getUsername();
        username = MagicAPIConfigUtils.getElasticSearchConfig(dataSourceInfo.getKey(), "username", username);
        String password = dataSourceInfo.getPassword();
        password = MagicAPIConfigUtils.getElasticSearchConfig(dataSourceInfo.getKey(), "password", password);
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            String finalUsername = username;
            String finalPassword = password;
            builder.setHttpClientConfigCallback(httpClientBuilder -> {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(finalUsername, finalPassword));
                return httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            });
        }
        String pathPrefix = MagicAPIConfigUtils.getElasticSearchConfig(dataSourceInfo.getKey(), "pathPrefix", dataSourceInfo.getPathPrefix());
        if (StringUtils.isNotBlank(pathPrefix)) {
            builder.setPathPrefix(pathPrefix);
        }

        String other = MagicAPIConfigUtils.getElasticSearchConfig(dataSourceInfo.getKey(), "other", dataSourceInfo.getOther());
        if (StringUtils.isNotBlank(other)) {
            setAdditionalConfig(builder, other);
        }
        return builder.build();
    }


    /**
     * 构建ElasticSearch REST客户端并将其存储到客户端映射中
     * @param dataSourceInfo ElasticSearch数据源信息，包含连接所需的配置参数
     */
    public void buildElasticSearchRestClient(ElasticSearchDataSourceInfo dataSourceInfo){
        RestClient restClient = createElasticSearchRestClient(dataSourceInfo);
        RestClient oldClient = elasticSearchRestClientMap.put(dataSourceInfo.getKey(), restClient);
        if (oldClient != null) {
            destroyElasticSearchRestClient(oldClient);
        }
        elasticSearchDataSourceInfoMap.put(dataSourceInfo.getKey(), dataSourceInfo);
    }

    /**
     * 销毁ElasticSearch REST客户端，关闭客户端连接
     * @param restClient 要销毁的REST客户端实例
     */
    public void destroyElasticSearchRestClient(RestClient restClient){
        if (restClient != null) {
            try {
                // 检查客户端是否正在运行，如果运行中则关闭连接
                if (restClient.isRunning()) {
                    restClient.close();
                }
            } catch (IOException e) {
                log.error("Failed to close ElasticSearch REST client", e);
            }
        }
    }

    public RestClient getElasticSearchRestClient(String key) {
        return elasticSearchRestClientMap.get(key);
    }


    /**
     * 获取数据源节点列表
     * 从elasticSearchDataSourceInfoMap中获取所有数据源信息，并检查是否存在运行中的默认数据源，
     * 如果存在则添加默认数据源信息到列表中
     *
     * @return 包含所有数据源信息的列表，包括从map中获取的数据源和可能存在的默认数据源
     */
    public List<ElasticSearchDataSourceInfo> datasourceNodes() {
        // 从elasticSearchDataSourceInfoMap中获取所有数据源信息并转换为列表
        List<ElasticSearchDataSourceInfo>  dataSourceInfoList = elasticSearchDataSourceInfoMap.values().stream().map(it -> {
            return it;
        }).collect(Collectors.toList());
        try {
            // 尝试获取RestClient实例并检查其运行状态
            RestClient restClient = SpringUtils.getBean(RestClient.class);
            if (restClient != null && restClient.isRunning()) {
                Response response = restClient.performRequest(new Request("GET", "/_cluster/health"));
                if (response.getStatusLine().getStatusCode() == 200) {
                    // 创建默认数据源信息并添加到列表中
                    ElasticSearchDataSourceInfo dataSourceInfo = new ElasticSearchDataSourceInfo();
                    dataSourceInfo.setKey("default");
                    dataSourceInfo.setName("默认数据源");
                    dataSourceInfoList.add(dataSourceInfo);
                }
            }
        } catch (Exception e) {
        }
        return dataSourceInfoList;
    }

    /**
     * 根据键值销毁对应的ElasticSearch REST客户端
     * @param key 客户端映射中的键值
     */
    public void destroyByKey(String  key) {
        RestClient restClient = elasticSearchRestClientMap.get(key);
        destroyElasticSearchRestClient(restClient);
        elasticSearchRestClientMap.remove(key);
        elasticSearchDataSourceInfoMap.remove(key);
    }

    /**
     * 销毁所有ElasticSearch REST客户端，实现销毁接口
     * @throws Exception 销毁过程中可能抛出的异常
     */
    @Override
    public void destroy() throws Exception {
        log.info("正在关闭所有 ElasticSearch 引擎");
        // 遍历客户端映射的所有键，逐个销毁对应的客户端
        elasticSearchRestClientMap.keySet().forEach(this::destroyByKey);
        log.info("已关闭所有 ElasticSearch 引擎");
    }

}
