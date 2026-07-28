package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.model.MagicDynamicInfluxDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.model.InfluxDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class InfluxDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<InfluxDataSourceInfo> {


    /**
     * MagicDynamicInfluxDataSource实例，用于动态访问InfluxDB数据源
     * 该字段被声明为private final，确保线程安全性和不可变性
     */
    private final MagicDynamicInfluxDataSource magicDynamicInfluxDataSource;


    /**
     * 构造函数，用于初始化InfluxDataSourceMagicDynamicRegistry实例
     *
     * @param magicResourceStorage 用于存储Influx数据源信息的资源存储器
     * @param magicDynamicInfluxDataSource 动态Influx数据源实例，用于管理动态数据源连接
     */
    public InfluxDataSourceMagicDynamicRegistry(MagicResourceStorage<InfluxDataSourceInfo> magicResourceStorage, MagicDynamicInfluxDataSource magicDynamicInfluxDataSource) {
        super(magicResourceStorage);
        this.magicDynamicInfluxDataSource = magicDynamicInfluxDataSource;
    }



    /**
     * 处理文件事件的监听方法，当事件类型为'influx-datasource'时触发
     * @param event 文件事件对象，包含事件类型和其他相关信息
     */
    @EventListener(condition = "#event.type == 'influx-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            // 处理文件事件的核心逻辑
            processEvent(event);
        } catch (Exception ignored) {
        }
    }


    /**
     * 处理组事件的监听方法，当事件类型为'influx-datasource'时触发
     *
     * @param event 组事件对象，包含事件类型和其他相关信息
     */
    @EventListener(condition = "#event.type == 'influx-datasource'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }


    /**
     * 注册InfluxDB数据源信息
     *
     * @param entity InfluxDB数据源信息实体对象，包含连接配置等关键信息
     * @return boolean 注册成功返回true，失败返回false
     *
     * 该方法首先销毁已存在的同名InfluxDB客户端连接，然后执行父类的注册逻辑
     */
    @Override
    public boolean register(InfluxDataSourceInfo entity) {
        // 销毁已存在的同名InfluxDB客户端连接，确保使用最新的配置
        this.magicDynamicInfluxDataSource.destroyInfluxDBClientByKey(entity.getKey());
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }


    /**
     * 注册InfluxDB数据源映射节点
     *
     * @param mappingNode 包含InfluxDataSourceInfo实体的映射节点，用于注册数据源
     * @return 返回注册结果，true表示注册成功，false表示注册失败
     */
    @Override
    protected boolean register(MappingNode<InfluxDataSourceInfo> mappingNode) {
        InfluxDataSourceInfo entity = mappingNode.getEntity();
        // 销毁旧的InfluxDB客户端连接
        this.magicDynamicInfluxDataSource.destroyInfluxDBClientByKey(entity.getKey());
        // 构建新的InfluxDB客户端连接
        this.magicDynamicInfluxDataSource.buildInfluxDBClient(entity);
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }


    /**
     * 注销映射节点并清理相关资源
     *
     * @param mappingNode 映射节点对象，包含Influx数据源信息实体
     */
    @Override
    protected void unregister(MappingNode<InfluxDataSourceInfo> mappingNode) {
        // 销毁指定key对应的InfluxDB客户端连接
        this.magicDynamicInfluxDataSource.destroyInfluxDBClientByKey(mappingNode.getEntity().getKey());
        // 调用父类的注销方法完成剩余的注销逻辑
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }


    /**
     * 获取默认的数据源映射信息列表
     *
     * @return 默认数据源信息列表，包含名称和键值
     */
    @Override
    public List<InfluxDataSourceInfo> defaultMappings() {
        // 筛选出ID为空的数据源节点，并转换为InfluxDataSourceInfo对象
        return this.magicDynamicInfluxDataSource.datasourceNodes().stream().filter(it -> it.getId() == null).map(it -> {
            InfluxDataSourceInfo dataSourceInfo = new InfluxDataSourceInfo();
            // 设置数据源名称，优先使用name字段，其次使用key字段，最后使用默认值"默认数据源"
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
    }



}
