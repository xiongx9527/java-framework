package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.datasource.model.DataSourceInfo;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;
import org.ssssssss.magicapi.datasource.service.DataSourceMagicDynamicRegistry;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
public class MyDataSourceMagicDynamicRegistry extends DataSourceMagicDynamicRegistry {

    /**
     * 数据源构建器映射表，用于存储不同数据源名称对应的构建器实例
     * 使用ConcurrentHashMap保证线程安全
     */
    private final Map<String,DataSourceBuilder> dataSourceBuilderMap = new ConcurrentHashMap<>();

    /**
     * 默认数据源构建器，当没有找到特定数据源构建器时使用
     */
    private DefaultDataSourceBuilder defaultDataSourceBuilder = new DefaultDataSourceBuilder();

    /**
     * 魔法动态数据源实例，用于实现动态数据源切换功能
     */
    private final MagicDynamicDataSource magicDynamicDataSource;

    /**
     * 当前类的类加载器实例
     */
    public static final ClassLoader CLASSLOADER = MyDataSourceMagicDynamicRegistry.class.getClassLoader();

    /**
     * 支持的数据源类型名称数组
     * 从DataSourceBuilder复制而来，包含常用的数据库连接池实现类
     * HikariCP、Druid、Tomcat JDBC Pool、Commons DBCP2
     */
    // copy from DataSourceBuilder
    public static final String[] DATA_SOURCE_TYPE_NAMES = new String[]{
            "com.zaxxer.hikari.HikariDataSource",
            "com.alibaba.druid.pool.DruidDataSource",
            "org.apache.tomcat.jdbc.pool.DataSource",
            "org.apache.commons.dbcp2.BasicDataSource"};


    /**
     * 构造方法，初始化MyDataSourceMagicDynamicRegistry实例。
     *
     * @param magicResourceStorage      资源存储对象，用于管理数据源信息资源
     * @param magicDynamicDataSource    动态数据源对象，用于实际的数据源注册与切换
     * @param dataSourceBuilderMap      数据源构建器映射表，根据类型获取对应的数据源构建方式
     */
    public MyDataSourceMagicDynamicRegistry(MagicResourceStorage<DataSourceInfo> magicResourceStorage, MagicDynamicDataSource magicDynamicDataSource
            ,Map<String,DataSourceBuilder> dataSourceBuilderMap) {
        super(magicResourceStorage,magicDynamicDataSource);
        this.magicDynamicDataSource = magicDynamicDataSource;
        if (MapUtils.isNotEmpty(dataSourceBuilderMap)) {
            this.dataSourceBuilderMap.putAll(dataSourceBuilderMap);
        }
    }

    /**
     * 监听文件事件，当事件类型为'datasource'时处理该事件。
     * 处理过程中若发生异常将记录错误日志。
     *
     * @param event 文件事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 注册数据源信息
     * 重写父类的register方法，使用父类的注册逻辑来注册数据源信息
     *
     * @param entity 数据源信息实体对象，包含需要注册的数据源相关信息
     * @return boolean 返回注册结果，true表示注册成功，false表示注册失败
     */
    @Override
    public boolean register(DataSourceInfo entity) {
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }


    /**
     * 注册指定的数据源节点到动态数据源中。
     * 根据数据源信息构造属性集，并使用对应的构建器创建数据源实例后进行注册。
     *
     * @param mappingNode 包含数据源信息的映射节点
     * @return 注册是否成功（始终返回true）
     */
    @Override
    protected boolean register(MappingNode<DataSourceInfo> mappingNode) {
        DataSourceInfo info = mappingNode.getEntity();
        Map<String, Object> properties = new HashMap<>(info.getProperties());
        String other = MagicAPIConfigUtils.getDbConfig(info.getKey(), "other", "");
        if (StringUtils.isNotBlank(other)) {
            JSONValidator jsonValidator = JSONValidator.from(other);
            if (jsonValidator.validate() && jsonValidator.getType() == JSONValidator.Type.Object) {
                JSONObject jsonObject = JSON.parseObject(other);
                if (jsonObject != null && !jsonObject.isEmpty()) {
                    properties = jsonObject;
                }
            }
        }
        String url = MagicAPIConfigUtils.getDbConfig(info.getKey(), "url", info.getUrl());
        String username = MagicAPIConfigUtils.getDbConfig(info.getKey(), "username", info.getUsername());
        String password = MagicAPIConfigUtils.getDbConfig(info.getKey(), "password", info.getPassword());
        String type = MagicAPIConfigUtils.getDbConfig(info.getKey(), "type", info.getType());
        String driverClassName = MagicAPIConfigUtils.getDbConfig(info.getKey(), "driverClassName", info.getDriverClassName());
        // 特殊处理Elasticsearch驱动，设置只读模式
        if(Constants.ES_DRIVER.equals(driverClassName)){
            properties.put("defaultReadOnly", true);
        }
        // 设置基本连接属性
        properties.put("name", info.getName());
        properties.put("url", url);
        properties.put("username", username);
        properties.put("password", password);
        // 若未提供驱动类名，则尝试从URL推断
        if (StringUtils.isBlank(driverClassName)) {
            String driverClass = DatabaseDriver.fromJdbcUrl(url).getDriverClassName();
            properties.put("driverClassName", driverClass);
        } else {
            properties.put("driverClassName", driverClassName);
        }
        // 获取并使用合适的构建器来创建数据源
        DataSourceBuilder dataSourceBuilder = null;
        if (StringUtils.isNotBlank(type)) {
            dataSourceBuilder = dataSourceBuilderMap.get(type);
        }
        if (Objects.isNull(dataSourceBuilder)) {
            dataSourceBuilder = defaultDataSourceBuilder;
        }
        DataSource datasource = dataSourceBuilder.buildDataSource(type, properties);
        // 将数据源加入动态数据源容器中
        magicDynamicDataSource.put(info.getId(), info.getKey(), info.getName(), datasource, info.getMaxRows());
        return true;
    }

    /**
     * 取消注册指定的数据源节点。
     *
     * @param mappingNode 需要取消注册的数据源映射节点
     */
    @Override
    protected void unregister(MappingNode<DataSourceInfo> mappingNode) {
        magicDynamicDataSource.delete(mappingNode.getMappingKey());
    }

    /**
     * 获取默认的数据源映射列表。
     * 这些是系统内置或预定义但尚未分配ID的数据源节点。
     *
     * @return 默认数据源信息列表
     */
    @Override
    public List<DataSourceInfo> defaultMappings() {
        return magicDynamicDataSource.datasourceNodes().stream().filter(it -> it.getId() == null).map(it -> {
            DataSourceInfo dataSourceInfo = new DataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
    }


}
