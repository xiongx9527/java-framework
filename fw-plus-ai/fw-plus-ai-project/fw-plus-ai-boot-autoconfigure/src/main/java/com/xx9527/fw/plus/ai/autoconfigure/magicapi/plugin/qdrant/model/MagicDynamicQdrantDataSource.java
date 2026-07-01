package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.common.BaseService;
import com.xx9527.fw.plus.common.util.SpringUtils;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MagicDynamicQdrantDataSource implements BaseService, DisposableBean {

    /** 
     * 存储Qdrant客户端的映射表，键为数据源ID，值为对应的Qdrant客户端实例
     */
    private Map<String, QdrantClient> clientMap = new ConcurrentHashMap<>();

    /** 
     * 存储Qdrant数据源信息的映射表，键为数据源ID，值为对应的数据源信息
     */
    private Map<String, QdrantDataSourceInfo> infoMap = new ConcurrentHashMap<>();

    /**
     * 根据Qdrant数据源信息构建Qdrant客户端
     * 使用提供的数据源信息创建Qdrant gRPC客户端，并应用相关的配置选项，如API密钥和超时设置
     *
     * @param info Qdrant数据源信息
     * @return QdrantClient Qdrant客户端实例
     * @throws IllegalArgumentException 如果提供的数据源信息为空或主机地址/端口无效
     */
    public QdrantClient buildClient(QdrantDataSourceInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("Qdrant数据源信息不能为空");
        }
        String url = info.getHost() ;
        url = MagicAPIConfigUtils.getQdrantConfig(info.getKey(), "host", url);
        String portStr = info.getPort() ;
        portStr = MagicAPIConfigUtils.getQdrantConfig(info.getKey(), "port", portStr);
        int port = Integer.parseInt(portStr);
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(url, port, info.isUtls());
        String apikey = info.getApiKey();
        apikey = MagicAPIConfigUtils.getQdrantConfig(info.getKey(), "apiKey", apikey);
        if (StringUtils.isNotBlank(apikey)) {
            builder.withApiKey(apikey);
        }
        
        String other = info.getOther();
        other = MagicAPIConfigUtils.getQdrantConfig(info.getKey(), "other", other);
        if (StringUtils.isNotBlank(other)) {
            try {
                JSONObject otherConfig = JSON.parseObject(other);
                
                // 设置超时时间（如果配置中存在）
                if (otherConfig.containsKey("timeout")) {
                    Long timeout = otherConfig.getLong("timeout");
                    if (timeout != null && timeout > 0) {
                        builder.withTimeout(Duration.ofSeconds(timeout));
                    }
                }
                
                log.debug("Applied Qdrant other config for datasource: {}", info.getKey());
            } catch (com.alibaba.fastjson2.JSONException e) {
                log.warn("Failed to parse Qdrant other config JSON for datasource: {}", info.getKey(), e);
            } catch (Exception e) {
                log.warn("Failed to apply Qdrant other config for datasource: {}", info.getKey(), e);
            }
        }
        
        try {
            return new QdrantClient(builder.build());
        } catch (Exception e) {
            log.error("Failed to build Qdrant client for datasource: {}", info.getKey(), e);
            throw e;
        }
    }

    /**
     * 创建并存储Qdrant客户端
     * 根据提供的数据源信息构建Qdrant客户端，并将其与对应的数据源信息一起存储到内部映射表中
     *
     * @param info Qdrant数据源信息
     */
    public void createClient(QdrantDataSourceInfo info) {
        QdrantClient client = buildClient(info);
        this.clientMap.put(info.getKey(), client);
        this.infoMap.put(info.getKey(), info);
    }

    /**
     * 根据键获取Qdrant客户端
     * 从内部客户端映射表中查找指定键对应的Qdrant客户端实例
     *
     * @param key 客户端键
     * @return QdrantClient Qdrant客户端实例，如果未找到则返回null
     */
    public QdrantClient getClient(String key) {
        return this.clientMap.get(key);
    }

    /**
     * 销毁Qdrant客户端
     * 安全地关闭Qdrant客户端连接，释放相关资源
     * 方法会忽略重复关闭客户端的尝试，并在发生异常时记录警告日志
     *
     * @param client 需要销毁的Qdrant客户端
     */
    public void destroyClient(QdrantClient client) {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("关闭Qdrant客户端时发生异常", e);
            } 
        }
    }

    /**
     * 获取所有数据源节点信息
     * 包含动态创建的数据源和Spring容器中默认的Qdrant客户端（如果存在）
     *
     * @return List<QdrantDataSourceInfo> 数据源信息列表
     */
    public List<QdrantDataSourceInfo> datasourceNodes() {
        // 获取所有动态创建的数据源信息
        List<QdrantDataSourceInfo>  dataSourceInfoList = new ArrayList<>(infoMap.values());
        try {
            QdrantClient client = SpringUtils.getBean(QdrantClient.class);
            if (client != null) {
                QdrantDataSourceInfo dataSourceInfo = new QdrantDataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName("默认数据源");
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
        }
        return dataSourceInfoList;
    }

    /**
     * 根据键销毁客户端
     *
     * @param key 客户端键
     */
    public void destroyClientByKey(String  key) {
        if (key == null) {
            return;
        }
        try {
            QdrantClient client = this.clientMap.get(key);
            if (client != null) {
                try {
                    destroyClient(client);
                } catch (Exception e) {
                    log.warn("关闭Qdrant客户端时发生异常，key: {}", key, e);
                }
                this.clientMap.remove(key);
            }
            // 同时从infoMap中移除对应的数据源信息
            this.infoMap.remove(key);
        } catch (Exception e) {
            log.warn("销毁Qdrant客户端时发生异常，key: {}", key, e);
        }
    }


    /**
     * 销毁所有Qdrant客户端并清理资源
     * 实现DisposableBean接口的destroy方法，在容器关闭时调用此方法来释放所有Qdrant客户端资源
     *
     * @throws Exception 如果在关闭客户端过程中发生错误
     */
    @Override
    public void destroy() throws Exception {
        // 关闭所有Qdrant客户端
        // 创建key集合的副本以避免并发修改异常
        for (String key : new ArrayList<>(this.clientMap.keySet())) {
            try {
                destroyClientByKey(key);
            } catch (Exception e) {
                log.warn("关闭Qdrant客户端时发生异常，key: {}", key, e);
            }
        }
        
        // 清空clientMap和infoMap
        try {
            this.clientMap.clear();
            this.infoMap.clear();
        } catch (Exception e) {
            log.warn("清理Qdrant客户端映射时发生异常", e);
        }
    }

}
