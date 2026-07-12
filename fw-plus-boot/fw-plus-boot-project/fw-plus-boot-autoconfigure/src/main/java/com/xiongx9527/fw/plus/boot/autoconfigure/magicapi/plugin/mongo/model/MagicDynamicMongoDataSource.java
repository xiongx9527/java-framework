package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class MagicDynamicMongoDataSource implements DisposableBean {


    /**
     * 存储Mongo数据库工厂的线程安全映射表
     * 用于缓存和管理多个Mongo数据源的数据库工厂实例
     * key为数据源唯一标识符，value为对应的MongoDatabaseFactory实例
     */
    private static final Map<String, MongoDatabaseFactory> mongoDatabaseFactoryMap = new ConcurrentHashMap<>();

    /**
     * 存储Mongo数据源信息的线程安全映射表
     * 用于缓存和管理多个Mongo数据源的配置信息
     * key为数据源唯一标识符，value为对应的MongoDataSourceInfo实例
     */
    private static final Map<String, MongoDataSourceInfo> mongoDataSourceInfoMap = new ConcurrentHashMap<>();

    /**
     * 创建Mongo数据库工厂
     * 根据数据源信息创建相应的Mongo数据库工厂实例
     *
     * @param dataSourceInfo Mongo数据源信息，包含连接所需的各种配置参数
     * @return MongoDatabaseFactory Mongo数据库工厂实例
     * @throws RuntimeException 当创建MongoDatabaseFactory失败时抛出
     */
    public MongoDatabaseFactory createMongoDatabaseFactory(MongoDataSourceInfo dataSourceInfo) {
        // 检查数据源信息是否为空
        if (dataSourceInfo == null) {
            log.warn("数据源信息为空，无法构建Mongo连接工厂");
            throw new RuntimeException("数据源信息为空，无法构建Mongo连接工厂");
        }
        
        try {
            // 根据不同的模式创建MongoDatabaseFactory
            if ("cluster".equals(dataSourceInfo.getMode())) {
                // 集群模式配置
                return createClusterMongoDatabaseFactory(dataSourceInfo);
            } else if ("replicaSet".equals(dataSourceInfo.getMode())) {
                // 副本集模式配置
                return createReplicaSetMongoDatabaseFactory(dataSourceInfo);
            } else {
                // 默认使用单机模式
                return createStandaloneMongoDatabaseFactory(dataSourceInfo);
            }
        } catch (Exception e) {
            log.error("创建MongoDatabaseFactory失败: {}", dataSourceInfo.getKey(), e);
            throw new RuntimeException("创建MongoDatabaseFactory失败: " + dataSourceInfo.getKey(), e);
        }
    }

    /**
     * 构建并注册Mongo数据库工厂
     * 根据数据源信息创建Mongo数据库工厂，并将其注册到内部映射表中以便后续使用
     *
     * @param dataSourceInfo Mongo数据源信息，包含连接所需的各种配置参数
     */
    public void buildMongoDatabaseFactory(MongoDataSourceInfo dataSourceInfo) { 
        MongoDatabaseFactory mongoDatabaseFactory = createMongoDatabaseFactory(dataSourceInfo);
        MongoDatabaseFactory oldMongoDatabaseFactory = mongoDatabaseFactoryMap.put(dataSourceInfo.getKey(), mongoDatabaseFactory);
        if (oldMongoDatabaseFactory != null) {
            destroyMongoDatabaseFactory(oldMongoDatabaseFactory);
        }
        mongoDataSourceInfoMap.put(dataSourceInfo.getKey(), dataSourceInfo);
    }

    /**
     * 创建集群模式的Mongo数据库工厂
     *
     * @param dataSourceInfo Mongo数据源信息
     * @return MongoDatabaseFactory 集群模式的数据库工厂
     */
    private MongoDatabaseFactory createClusterMongoDatabaseFactory(MongoDataSourceInfo dataSourceInfo) {
        try {
            // 从other字段解析集群配置
            List<String> hosts = new ArrayList<>();
            String hostname = dataSourceInfo.getHostname();
            hostname = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "hostname", hostname);
            String portStr = dataSourceInfo.getPort();
            portStr = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "port", portStr);
            String database = dataSourceInfo.getDatabase();
            database = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "database", database);
            String username = dataSourceInfo.getUsername();
            username = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "username", username);
            String password = dataSourceInfo.getPassword();
            password = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "password", password);
            String other = dataSourceInfo.getOther();
            other = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "other", other);
            if (StringUtils.isNotBlank(other)) {
                try {
                    JSONObject jsonObject = JSON.parseObject(other);
                    JSONArray nodes = jsonObject.getJSONArray("nodes");
                    if (nodes != null) {
                        for (int i = 0; i < nodes.size(); i++) {
                            JSONObject node = nodes.getJSONObject(i);
                            String host = node.getString("host");
                            String port = node.getString("port");
                            if (StringUtils.isNotBlank(host)) {
                                if (StringUtils.isNotBlank(port)) {
                                    hosts.add(host + ":" + port);
                                } else {
                                    hosts.add(host);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("解析集群配置失败，使用默认配置: {}", other, e);
                }
            }

            // 如果没有从other字段解析到节点，则使用host字段
            if (hosts.isEmpty() && StringUtils.isNotBlank(hostname) && StringUtils.isNotBlank(portStr)) {
                hosts.add(hostname.trim() + ":" + portStr);
            }

            // 构建集群连接字符串
            StringBuilder connectionStringBuilder = new StringBuilder("mongodb://");

            // 添加认证信息
            if (StringUtils.isNotBlank(username)) {
                connectionStringBuilder.append(username);
                if (StringUtils.isNotBlank(password)) {
                    connectionStringBuilder.append(":").append(password);
                }
                connectionStringBuilder.append("@");
            }

            // 添加集群节点
            for (int i = 0; i < hosts.size(); i++) {
                if (i > 0) {
                    connectionStringBuilder.append(",");
                }
                connectionStringBuilder.append(hosts.get(i));
            }

            // 添加数据库
            if (StringUtils.isNotBlank(database)) {
                connectionStringBuilder.append("/").append(database);
            }

            // 添加集群选项
            connectionStringBuilder.append("?connect=replicaSet");

            ConnectionString connectionString = new ConnectionString(connectionStringBuilder.toString());
            MongoClient mongoClient = MongoClients.create(connectionString);
            return new SimpleMongoClientDatabaseFactory(mongoClient, database);
        } catch (Exception e) {
            log.error("创建集群模式MongoDatabaseFactory失败: {}", dataSourceInfo.getKey(), e);
            throw new RuntimeException("创建集群模式MongoDatabaseFactory失败: " + dataSourceInfo.getKey(), e);
        }
    }

    /**
     * 创建副本集模式的Mongo数据库工厂
     *
     * @param dataSourceInfo Mongo数据源信息
     * @return MongoDatabaseFactory 副本集模式的数据库工厂
     */
    private MongoDatabaseFactory createReplicaSetMongoDatabaseFactory(MongoDataSourceInfo dataSourceInfo) {
        try {
            // 从other字段解析副本集配置
            List<String> hosts = new ArrayList<>();
            String hostname = dataSourceInfo.getHostname();
            hostname = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "hostname", hostname);
            String portStr = dataSourceInfo.getPort();
            portStr = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "port", portStr);
            String database = dataSourceInfo.getDatabase();
            database = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "database", database);
            String username = dataSourceInfo.getUsername();
            username = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "username", username);
            String password = dataSourceInfo.getPassword();
            password = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "password", password);
            String other = dataSourceInfo.getOther();
            other = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "other", other);

            // 获取副本集名称和读偏好
            String replicaSet = "";
            String readPreference = ""; // 添加读偏好变量
            if (StringUtils.isNotBlank(other)) {
                try {
                    JSONObject jsonObject = JSON.parseObject(other);
                    JSONArray nodes = jsonObject.getJSONArray("nodes");
                    if (nodes != null) {
                        for (int i = 0; i < nodes.size(); i++) {
                            JSONObject node = nodes.getJSONObject(i);
                            String host = node.getString("host");
                            String port = node.getString("port");
                            if (StringUtils.isNotBlank(host)) {
                                if (StringUtils.isNotBlank(port)) {
                                    hosts.add(host + ":" + port);
                                } else {
                                    hosts.add(host);
                                }
                            }
                        }
                    }

                    // 获取认证信息
                    String otherUsername = jsonObject.getString("username");
                    String otherPassword = jsonObject.getString("password");
                    if (StringUtils.isNotBlank(otherUsername)) {
                        username = otherUsername;
                    }
                    if (StringUtils.isNotBlank(otherPassword)) {
                        password = otherPassword;
                    }

                    // 获取副本集名称
                    replicaSet = jsonObject.getString("replicaSet");
                    
                    // 获取读偏好配置
                    readPreference = jsonObject.getString("readPreference");
                } catch (Exception e) {
                    log.warn("解析副本集配置失败，使用默认配置: {}", other, e);
                }
            }

            // 如果没有从other字段解析到节点，则使用host字段
            if (hosts.isEmpty() && StringUtils.isNotBlank(hostname) && StringUtils.isNotBlank(portStr)) {
                hosts.add(hostname.trim() + ":" + portStr);
            }

            // 构建副本集连接字符串
            StringBuilder connectionStringBuilder = new StringBuilder("mongodb://");

            // 添加认证信息
            if (StringUtils.isNotBlank(username)) {
                connectionStringBuilder.append(username);
                if (StringUtils.isNotBlank(password)) {
                    connectionStringBuilder.append(":").append(password);
                }
                connectionStringBuilder.append("@");
            }

            // 添加副本集节点
            for (int i = 0; i < hosts.size(); i++) {
                if (i > 0) {
                    connectionStringBuilder.append(",");
                }
                connectionStringBuilder.append(hosts.get(i));
            }

            // 添加数据库
            if (StringUtils.isNotBlank(database)) {
                connectionStringBuilder.append("/").append(database);
            }

            // 添加副本集选项
            boolean hasOptions = false;
            if (StringUtils.isNotBlank(replicaSet)) {
                connectionStringBuilder.append("?replicaSet=").append(replicaSet);
                hasOptions = true;
            }
            
            // 添加读偏好配置
            if (StringUtils.isNotBlank(readPreference)) {
                connectionStringBuilder.append(hasOptions ? "&" : "?")
                        .append("readPreference=").append(readPreference);
            }

            ConnectionString connectionString = new ConnectionString(connectionStringBuilder.toString());
            MongoClient mongoClient = MongoClients.create(connectionString);
            return new SimpleMongoClientDatabaseFactory(mongoClient, database);
        } catch (Exception e) {
            log.error("创建副本集模式MongoDatabaseFactory失败: {}", dataSourceInfo.getKey(), e);
            throw new RuntimeException("创建副本集模式MongoDatabaseFactory失败: " + dataSourceInfo.getKey(), e);
        }
    }

    /**
     * 创建单机模式的Mongo数据库工厂
     *
     * @param dataSourceInfo Mongo数据源信息
     * @return MongoDatabaseFactory 单机模式的数据库工厂
     */
    private MongoDatabaseFactory createStandaloneMongoDatabaseFactory(MongoDataSourceInfo dataSourceInfo) {
        try {
            String hostname = dataSourceInfo.getHostname();
            hostname = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "hostname", hostname);
            String portStr = dataSourceInfo.getPort();
            portStr = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "port", portStr);
            String database = dataSourceInfo.getDatabase();
            database = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "database", database);
            String username = dataSourceInfo.getUsername();
            username = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "username", username);
            String password = dataSourceInfo.getPassword();
            password = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "password", password);
            String other = dataSourceInfo.getOther();
            other = MagicAPIConfigUtils.getMongoConfig(dataSourceInfo.getKey(), "other", other);

            // 从other字段解析额外配置
            if (StringUtils.isNotBlank(other)) {
                try {
                    JSONObject jsonObject = JSON.parseObject(other);

                    // 获取主机和端口
                    String otherHost = jsonObject.getString("host");
                    String otherPort = jsonObject.getString("port");
                    if (StringUtils.isNotBlank(otherHost)) {
                        hostname = otherHost;
                    }
                    if (otherPort != null && StringUtils.isNotBlank(otherPort)) {
                        portStr = otherPort;
                    }

                    // 获取数据库名
                    String otherDatabase = jsonObject.getString("database");
                    if (StringUtils.isNotBlank(otherDatabase)) {
                        database = otherDatabase;
                    }

                    // 获取认证信息
                    String otherUsername = jsonObject.getString("username");
                    String otherPassword = jsonObject.getString("password");
                    if (StringUtils.isNotBlank(otherUsername)) {
                        username = otherUsername;
                    }
                    if (StringUtils.isNotBlank(otherPassword)) {
                        password = otherPassword;
                    }
                } catch (Exception e) {
                    log.warn("解析单机配置失败，使用默认配置: {}", other, e);
                }
            }

            // 构建单机连接字符串
            StringBuilder connectionStringBuilder = new StringBuilder("mongodb://");

            // 添加认证信息
            if (StringUtils.isNotBlank(username)) {
                connectionStringBuilder.append(username);
                if (StringUtils.isNotBlank(password)) {
                    connectionStringBuilder.append(":").append(password);
                }
                connectionStringBuilder.append("@");
            }

            // 添加主机和端口
            if (StringUtils.isNotBlank(hostname)) {
                connectionStringBuilder.append(hostname.trim());
                if (StringUtils.isNotBlank(portStr)) {
                    connectionStringBuilder.append(":").append(portStr);
                }
            }

            // 添加数据库
            if (StringUtils.isNotBlank(database)) {
                connectionStringBuilder.append("/").append(database);
            }

            ConnectionString connectionString = new ConnectionString(connectionStringBuilder.toString());
            MongoClient mongoClient = MongoClients.create(connectionString);
            return new SimpleMongoClientDatabaseFactory(mongoClient, database);
        } catch (Exception e) {
            log.error("创建单机模式MongoDatabaseFactory失败: {}", dataSourceInfo.getKey(), e);
            throw new RuntimeException("创建单机模式MongoDatabaseFactory失败: " + dataSourceInfo.getKey(), e);
        }
    }

    /**
     * 销毁Mongo数据库工厂
     * 安全地关闭Mongo数据库工厂及相关资源
     *
     * @param mongoDatabaseFactory 需要被销毁的Mongo数据库工厂实例
     */
    public void destroyMongoDatabaseFactory(MongoDatabaseFactory mongoDatabaseFactory) {
        if (mongoDatabaseFactory != null) {
            try {
                // 通过反射获取MongoClient并关闭
                if (mongoDatabaseFactory instanceof SimpleMongoClientDatabaseFactory) {
                    try {
                        Method getMongoClientMethod = SimpleMongoClientDatabaseFactory.class.getDeclaredMethod("getMongoClient");
                        getMongoClientMethod.setAccessible(true);
                        MongoClient mongoClient = (MongoClient) getMongoClientMethod.invoke(mongoDatabaseFactory);
                        if (mongoClient != null) {
                            mongoClient.close();
                        }
                    } catch (Exception e) {
                        log.warn("通过反射获取或关闭MongoClient时发生异常", e);
                    }
                }
            } catch (Exception e) {
                log.warn("关闭MongoClient时发生异常", e);
            }
        }
    }

    /**
     * 根据指定的key销毁对应的Mongo数据库工厂及相关资源
     * 该方法会安全地关闭Mongo数据库工厂，并从映射表中移除该工厂及对应的数据源信息
     *
     * @param key Mongo数据库工厂的唯一标识符
     */
    private void destroyByKey(String key) {
        // 添加空值检查
        if (key == null) {
            log.warn("尝试销毁Mongo连接工厂时，key为空");
            return;
        }

        // 使用原子操作获取并移除连接工厂
        MongoDatabaseFactory mongoDatabaseFactory = mongoDatabaseFactoryMap.remove(key);
        if (mongoDatabaseFactory != null) {
            try {
                destroyMongoDatabaseFactory(mongoDatabaseFactory);
            } catch (Exception e) {
                log.error("销毁Mongo连接工厂时发生错误: {}", e.getMessage(), e);
            }
        }
        mongoDataSourceInfoMap.remove(key);
    }

    /**
     * 根据指定的key获取Redis连接工厂
     * 该方法从内部维护的映射表中查找并返回与指定key关联的Redis连接工厂实例
     *
     * @param key Redis连接工厂的唯一标识符
     * @return 与指定key关联的RedisConnectionFactory实例，如果未找到则返回null
     */
    public MongoDatabaseFactory getMongoDatabaseFactory(String key) {
        return mongoDatabaseFactoryMap.get(key);
    }


    /**
     * 获取所有数据源节点信息列表
     * 收集所有已注册的Mongo数据源信息，并添加一个默认数据源到列表中
     *
     * @return Mongo数据源信息列表，包含所有已注册的数据源和一个默认数据源
     */
    public List<MongoDataSourceInfo> datasourceNodes() {
        List<MongoDataSourceInfo>  dataSourceInfoList = mongoDataSourceInfoMap.values().stream().map(it -> {
            return it;
        }).collect(Collectors.toList());
        try {
            MongoDatabaseFactory mongoDatabaseFactory = SpringUtils.getBean(MongoDatabaseFactory.class);
            if (mongoDatabaseFactory != null) {
                MongoDataSourceInfo dataSourceInfo = new MongoDataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName("默认数据源");
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
        }
        return dataSourceInfoList;
    }


    /**
     * 根据键销毁Mongo数据库工厂
     * 通过指定的键从映射表中移除对应的Mongo数据库工厂，并释放相关资源
     *
     * @param key Mongo数据库工厂的唯一标识符
     */
    public void destroyMongoConnectionFactoryByKey(String  key) {
        destroyByKey(key);
    }

    /**
     * 销毁方法，用于清理资源
     * 该方法会遍历mongoDataSourceInfoMap中的所有键，并对每个键调用destroyByKey方法进行资源销毁
     *
     * @throws Exception 当销毁过程中发生异常时抛出
     */
    @Override
    public void destroy() throws Exception {
        // 遍历所有数据源信息的键，逐个销毁对应的资源
        mongoDataSourceInfoMap.keySet().forEach(this::destroyByKey);
    }

}
