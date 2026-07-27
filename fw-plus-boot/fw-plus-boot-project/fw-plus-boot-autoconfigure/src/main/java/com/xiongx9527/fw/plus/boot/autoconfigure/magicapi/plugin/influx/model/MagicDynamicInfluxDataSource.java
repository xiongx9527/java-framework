package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.model;

import com.alibaba.fastjson.JSON;
import com.influxdb.LogLevel;
import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.InfluxDBClientOptions;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class MagicDynamicInfluxDataSource implements DisposableBean {


    /**
     * InfluxDB客户端映射表
     * 用于存储不同数据源对应的InfluxDB客户端实例
     * key: 数据源标识符
     * value: InfluxDB客户端实例
     */
    private static final Map<String, InfluxDBClient> influxDBClientMap = new ConcurrentHashMap<>();


    /**
     * Influx数据源信息映射表
     * 用于存储不同数据源的连接配置信息
     * key: 数据源标识符
     * value: Influx数据源信息对象
     */
    private static final Map<String, InfluxDataSourceInfo> influxDataSourceInfoMap = new ConcurrentHashMap<>();



    /**
     * 根据数据源信息创建并配置一个 InfluxDB 客户端实例。
     *
     * @param dataSourceInfo 包含连接 InfluxDB 所需的信息，如 URL、认证信息、组织、存储桶等。
     * @return 配置完成的 InfluxDBClient 实例。
     */
    public InfluxDBClient createInfluxDBClient(InfluxDataSourceInfo dataSourceInfo) {
        InfluxDBClientOptions.Builder optionsBuilder = InfluxDBClientOptions.builder();
        String url = dataSourceInfo.getUrl();
        url = MagicAPIConfigUtils.getInfluxConfig(dataSourceInfo.getKey(), "url", url);
        optionsBuilder.url(url);

        String username = dataSourceInfo.getUsername();
        username = MagicAPIConfigUtils.getInfluxConfig(dataSourceInfo.getKey(), "username", username);
        String password = dataSourceInfo.getPassword();
        password = MagicAPIConfigUtils.getInfluxConfig(dataSourceInfo.getKey(), "password", password);
        String token = dataSourceInfo.getToken();
        token = MagicAPIConfigUtils.getInfluxConfig(dataSourceInfo.getKey(), "token", token);
        // 认证配置：优先使用用户名密码，其次使用 Token
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            optionsBuilder.authenticate(username, password.toCharArray());
        } else if (StringUtils.isNotBlank(token)) {
            optionsBuilder.authenticateToken(token.toCharArray());
        }

        String org = dataSourceInfo.getOrg();
        org = MagicAPIConfigUtils.getInfluxConfig(dataSourceInfo.getKey(), "org", org);
        // 组织和存储桶配置
        if (StringUtils.isNotBlank(org)) {
            optionsBuilder.org(org);
        }
        String bucket = dataSourceInfo.getBucket();
        bucket = MagicAPIConfigUtils.getInfluxConfig(dataSourceInfo.getKey(), "bucket", bucket);
        if (StringUtils.isNotBlank(bucket)) {
            optionsBuilder.bucket(bucket);
        }

        String logLevel = dataSourceInfo.getLogLevel();
        logLevel = MagicAPIConfigUtils.getInfluxConfig(dataSourceInfo.getKey(), "logLevel", logLevel);
        // 日志级别配置
        if ("NONE".equals(logLevel)) {
            optionsBuilder.logLevel(LogLevel.NONE);
        } else if ("BASIC".equals(logLevel)) {
            optionsBuilder.logLevel(LogLevel.BASIC);
        } else if ("HEADERS".equals(logLevel)) {
            optionsBuilder.logLevel(LogLevel.HEADERS);
        } else if ("BODY".equals(logLevel)) {
            optionsBuilder.logLevel(LogLevel.BODY);
        }

        String configuredProperties = MagicAPIConfigUtils.getInfluxConfig(dataSourceInfo.getKey(), "properties", null);
        Map<String, Object> properties = MagicAPIConfigUtils.mergeProperties(dataSourceInfo.getProperties(), configuredProperties);
        // 其他配置项处理，例如默认标签（defaultTag）
        if (properties.containsKey("defaultTag")) {
            Object defaultTag = properties.get("defaultTag");
            if (defaultTag instanceof Map) {
                for (Map.Entry<?, ?> entry : ((Map<?, ?>) defaultTag).entrySet()) {
                    if (entry.getKey() != null && entry.getValue() != null) {
                        optionsBuilder.addDefaultTag(entry.getKey().toString(), entry.getValue().toString());
                    }
                }
            }
        }
        return InfluxDBClientFactory.create(optionsBuilder.build());
    }

    /**
     * 构建并存储InfluxDB客户端
     *
     * @param dataSourceInfo 数据源信息对象，包含连接InfluxDB所需的信息
     */
    public void buildInfluxDBClient(InfluxDataSourceInfo dataSourceInfo) {
        // 创建InfluxDB客户端实例
        InfluxDBClient influxDBClient = createInfluxDBClient(dataSourceInfo);
        String key = dataSourceInfo.getKey();
        // 将客户端和数据源信息分别存储到对应的映射表中
        this.influxDBClientMap.put(key, influxDBClient);
        this.influxDataSourceInfoMap.put(key, dataSourceInfo);
    }


    /**
     * 销毁InfluxDB客户端连接
     * <p>
     * 该方法用于安全地关闭和销毁InfluxDB客户端实例，释放相关资源。
     *
     * @param influxDBClient InfluxDB客户端实例，可能为null
     */
    public void destroyInfluxDBClient(InfluxDBClient influxDBClient) {
        // 如果客户端实例不为null，则尝试关闭连接
        if (influxDBClient != null) {
            try {
                influxDBClient.close();
            } catch (Exception e) {
                log.error("Failed to close InfluxDB client", e);
            }
        }
    }


    /**
     * 根据键值销毁对应的InfluxDB客户端连接和数据源信息
     * @param key 需要销毁的客户端对应的键值，如果为null则直接返回
     */
    private void destroyByKey(String key) {
        // 添加空值检查
        if (key == null) {
            return;
        }

        // 从映射中移除并获取对应的InfluxDB客户端
        InfluxDBClient influxDBClient = this.influxDBClientMap.remove(key);
        if (influxDBClient != null) {
            try {
                // 销毁InfluxDB客户端连接
                destroyInfluxDBClient(influxDBClient);
            } catch (Exception e) {
                log.error("Failed to destroy InfluxDB client", e);
            }
        }

        // 移除对应的数据源信息
        this.influxDataSourceInfoMap.remove(key);
    }

    /**
     * 根据指定的键获取InfluxDB客户端实例
     *
     * @param key 用于查找InfluxDB客户端的键值
     * @return 返回与指定键关联的InfluxDB客户端实例，如果未找到则返回null
     */
    public InfluxDBClient getInfluxDBClient(String key) {
        return this.influxDBClientMap.get(key);
    }


    /**
     * 获取所有数据源节点信息列表
     *
     * @return 包含所有数据源信息的列表，包括配置的数据源和默认数据源
     */
    public List<InfluxDataSourceInfo> datasourceNodes() {
        // 从数据源信息映射表中提取所有数据源信息
        List<InfluxDataSourceInfo>  dataSourceInfoList = this.influxDataSourceInfoMap.values().stream().map(it -> {
            return it;
        }).collect(Collectors.toList());

        // 尝试获取默认数据源并添加到列表中
        try {
            InfluxDBClient influxDBClient = SpringUtils.getBean(InfluxDBClient.class);
            if (influxDBClient != null) {
                InfluxDataSourceInfo dataSourceInfo = new InfluxDataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName("默认数据源");
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
        }
        return dataSourceInfoList;
    }

    /**
     * 根据指定的键销毁InfluxDB客户端连接
     *
     * @param key 用于标识和定位InfluxDB客户端实例的键值
     */
    public void destroyInfluxDBClientByKey(String  key) {
        destroyByKey(key);
    }



    /**
     * 销毁方法，用于清理资源
     * <p>
     * 该方法会遍历所有的InfluxDB客户端实例并执行销毁操作，
     * 确保所有相关的资源得到正确释放。
     *
     * @throws Exception 如果在销毁过程中发生错误
     */
    @Override
    public void destroy() throws Exception {
        // 遍历所有键并执行销毁操作
        this.influxDBClientMap.keySet().forEach(this::destroyByKey);
    }



}
