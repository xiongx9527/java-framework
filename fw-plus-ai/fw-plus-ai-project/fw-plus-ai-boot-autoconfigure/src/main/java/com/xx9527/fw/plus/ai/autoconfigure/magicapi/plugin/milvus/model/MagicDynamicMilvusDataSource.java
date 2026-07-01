package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.common.BaseService;
import com.xx9527.fw.plus.common.util.SpringUtils;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.ReleaseCollectionReq;
import io.milvus.v2.service.collection.response.ListCollectionsResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MagicDynamicMilvusDataSource implements BaseService, DisposableBean {

    /** 
     * 存储Milvus客户端实例的线程安全映射
     * key为数据源标识，value为对应的Milvus客户端实例
     */
    private Map<String, MilvusClientV2> clientMap = new ConcurrentHashMap<>();

    /** 
     * 存储Milvus数据源信息的线程安全映射
     * key为数据源标识，value为对应的数据源信息
     */
    private Map<String, MilvusDataSourceInfo> infoMap = new ConcurrentHashMap<>();

    /**
     * 根据Milvus数据源信息构建连接配置
     * 解析数据源信息中的基本连接参数和额外配置，构建Milvus客户端连接配置对象
     *
     * @param info Milvus数据源信息，包含连接所需的基本信息和额外配置
     * @return ConnectConfig 连接配置对象，用于创建Milvus客户端
     */
    private ConnectConfig buildConnectConfig(MilvusDataSourceInfo info) {
        if (info == null) {
            return null;
        }

        ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder();
        String uri = MagicAPIConfigUtils.getMilvusConfig(info.getKey(), "uri", info.getUri());
        String token = MagicAPIConfigUtils.getMilvusConfig(info.getKey(), "token", info.getToken());
        if (StringUtils.isNotBlank(token)) {
            builder.token(token);
        }
        String username = MagicAPIConfigUtils.getMilvusConfig(info.getKey(), "username", info.getUsername());
        String password = MagicAPIConfigUtils.getMilvusConfig(info.getKey(), "password", info.getPassword());
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            builder.username(username);
            builder.password(password);
        }
        String dbName = MagicAPIConfigUtils.getMilvusConfig(info.getKey(), "dbName", info.getDbName());
        if (StringUtils.isNotBlank(dbName)) {
            builder.dbName(dbName);
        }

        String other = info.getOther();
        other = MagicAPIConfigUtils.getMilvusConfig(info.getKey(), "other", other);
        // 从other字段中解析额外配置
        if (StringUtils.isNotBlank(other)) {
            try {
                JSONObject jsonObject = JSON.parseObject(other);
                // 其他可能的配置项
                Boolean secure = jsonObject.getBoolean("secure");
                if (secure != null) {
                    builder.secure(secure);
                }

                Integer connectTimeoutMs = jsonObject.getInteger("connectTimeoutMs");
                if (connectTimeoutMs != null) {
                    builder.connectTimeoutMs(connectTimeoutMs);
                }

                Integer keepAliveTimeMs = jsonObject.getInteger("keepAliveTimeMs");
                if (keepAliveTimeMs != null) {
                    builder.keepAliveTimeMs(keepAliveTimeMs);
                }

                Integer idleTimeoutMs = jsonObject.getInteger("idleTimeoutMs");
                if (idleTimeoutMs != null) {
                    builder.idleTimeoutMs(idleTimeoutMs);
                }

                Boolean keepAliveWithoutCalls = jsonObject.getBoolean("keepAliveWithoutCalls");
                if (keepAliveWithoutCalls != null) {
                    builder.keepAliveWithoutCalls(keepAliveWithoutCalls);
                }

                Integer rpcDeadlineMs = jsonObject.getInteger("rpcDeadlineMs");
                if (rpcDeadlineMs != null) {
                    builder.rpcDeadlineMs(rpcDeadlineMs);
                }

                String serverName = jsonObject.getString("serverName");
                if (serverName != null && !serverName.isEmpty()) {
                    builder.serverName(serverName);
                }

                String serverPemPath = jsonObject.getString("serverPemPath");
                if (serverPemPath != null && !serverPemPath.isEmpty()) {
                    builder.serverPemPath(serverPemPath);
                }

                String clientPemPath = jsonObject.getString("clientPemPath");
                if (clientPemPath != null && !clientPemPath.isEmpty()) {
                    builder.clientPemPath(clientPemPath);
                }

                String caPemPath = jsonObject.getString("caPemPath");
                if (caPemPath != null && !caPemPath.isEmpty()) {
                    builder.caPemPath(caPemPath);
                }

            } catch (Exception e) {
                log.warn("解析Milvus连接配置失败: {}", info.getOther(), e);
            }
        }

        return builder.build();
    }

    /**
     * 根据Milvus数据源信息构建Milvus客户端
     * 使用数据源信息构建连接配置，并基于该配置创建新的Milvus客户端实例
     *
     * @param info Milvus数据源信息
     * @return MilvusClientV2 Milvus客户端实例
     */
    public MilvusClientV2 buildClient(MilvusDataSourceInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("Milvus数据源信息不能为空");
        }
        try {
            ConnectConfig connectConfig = buildConnectConfig(info);
            if (connectConfig == null) {
                throw new IllegalStateException("无法构建Milvus连接配置");
            }
            return new MilvusClientV2(connectConfig);
        } catch (Exception e) {
            log.error("构建Milvus客户端失败: {}", info.getId(), e);
            throw e;
        }
    }

    /**
     * 创建并存储Milvus客户端
     * 根据提供的数据源信息构建一个新的Milvus客户端，并将其存储在clients映射中
     *
     * @param info Milvus数据源信息，用于创建客户端
     */
    public void createClient(MilvusDataSourceInfo info) {
        if (info == null) {
            throw new IllegalArgumentException("Milvus数据源信息不能为空");
        }
        
        if (this.clientMap.containsKey(info.getId())) {
            log.warn("Milvus客户端已存在，ID: {}", info.getId());
            return;
        }
        
        try {
            MilvusClientV2 client = buildClient(info);
            this.clientMap.put(info.getKey(), client);
            this.infoMap.put(info.getKey(), info);
        } catch (Exception e) {
            log.error("创建Milvus客户端失败: {}", info.getId(), e);
            throw e;
        }
    }


    /**
     * 根据key获取Milvus客户端
     * 从clientMap中获取指定key对应的Milvus客户端实例
     *
     * @param key 客户端的标识key
     * @return Milvus客户端实例，如果不存在则返回null
     */
    public MilvusClientV2 getClient(String key) {
        return this.clientMap.get(key);
    }


    /**
     * 销毁指定的Milvus客户端
     * 在关闭客户端之前，先释放所有集合的资源，然后关闭客户端
     *
     * @param client 要销毁的Milvus客户端实例
     */
    public void destroyClient(MilvusClientV2 client) {
        if (client != null) {
            // 释放所有集合资源
            try {
                ListCollectionsResp listCollectionsResp = client.listCollections();
                for (String collectionName : listCollectionsResp.getCollectionNames()) {
                    try {
                        ReleaseCollectionReq releaseCollectionReq = ReleaseCollectionReq.builder()
                                .collectionName(collectionName)
                                .build();
                        client.releaseCollection(releaseCollectionReq);
                        log.debug("成功释放集合资源: {}", collectionName);
                    } catch (Exception e) {
                        log.warn("释放集合资源失败，集合名: {}", collectionName, e);
                    }
                }
            } catch (Exception e) {
                log.warn("获取集合列表失败，跳过集合资源释放", e);
            }

            try {
                client.close(getMaxWaitSeconds());
                log.debug("成功关闭Milvus客户端");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // 恢复中断状态
                log.warn("关闭Milvus客户端时被中断", e);
            } catch (Exception e) {
                log.warn("关闭Milvus客户端时发生异常", e);
            } 
        }
    }

    /**
     * 获取所有数据源信息列表
     * 返回当前存储的所有Milvus数据源信息的列表，包括动态创建的数据源和Spring容器中的默认数据源（如果存在）
     *
     * @return Milvus数据源信息列表
     */
    public List<MilvusDataSourceInfo> datasourceNodes() {
        // 获取所有动态创建的数据源信息
        List<MilvusDataSourceInfo>  dataSourceInfoList = new ArrayList<>(infoMap.values());
        try {
            MilvusClientV2 client = SpringUtils.getBean(MilvusClientV2.class);
            if (client != null) {
                MilvusDataSourceInfo dataSourceInfo = new MilvusDataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName("默认数据源");
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
        }
        return dataSourceInfoList;
    }

    /**
     * 根据key销毁Milvus客户端
     * 在关闭客户端之前，先释放所有集合的资源，然后关闭指定的Milvus客户端并从clients映射中移除
     *
     * @param key Milvus客户端的标识key
     */
    public void destroyClientByKey(String  key) {
        if (key == null) {
            return;
        }
        try {
            MilvusClientV2 client = this.clientMap.get(key);
            if (client != null) {
                try {
                    destroyClient(client);
                } catch (Exception e) {
                    log.warn("关闭Milvus客户端时发生异常，key: {}", key, e);
                }
                this.clientMap.remove(key);
            }
            // 同时从infoMap中移除对应的数据源信息
            this.infoMap.remove(key);
        } catch (Exception e) {
            log.warn("销毁Milvus客户端时发生异常，key: {}", key, e);
        }
    }

    /**
     * 销毁数据源，释放所有资源
     * 关闭所有Milvus客户端并清空clientMap和infoMap映射
     *
     * @throws Exception 如果在销毁过程中发生异常
     */
    @Override
    public void destroy() throws Exception {
        // 关闭所有Milvus客户端
        for (String key : this.clientMap.keySet()) {
            destroyClientByKey(key);
        }
        
        // 清空clientMap和infoMap
        this.clientMap.clear();
        this.infoMap.clear();
    }

}
