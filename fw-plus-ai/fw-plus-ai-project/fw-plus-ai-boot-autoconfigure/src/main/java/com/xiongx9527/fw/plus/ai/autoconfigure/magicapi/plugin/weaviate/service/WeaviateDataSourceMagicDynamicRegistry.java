package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.model.MagicDynamicWeaviateDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.model.WeaviateDataSourceInfo;
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
public class WeaviateDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<WeaviateDataSourceInfo> {

    /**
     * MagicDynamicWeaviateDataSource实例，用于与Weaviate数据源进行交互
     * 该数据源支持动态查询和数据操作功能
     */
    private final MagicDynamicWeaviateDataSource magicDynamicWeaviateDataSource;

    /**
     * 构造函数，初始化Weaviate数据源的动态注册器
     *
     * @param magicResourceStorage 用于存储Weaviate数据源信息的资源存储器
     * @param magicDynamicWeaviateDataSource 动态Weaviate数据源实例，用于处理动态数据源操作
     */
    public WeaviateDataSourceMagicDynamicRegistry(MagicResourceStorage<WeaviateDataSourceInfo> magicResourceStorage,
                                                  MagicDynamicWeaviateDataSource magicDynamicWeaviateDataSource) {
        super(magicResourceStorage);
        this.magicDynamicWeaviateDataSource = magicDynamicWeaviateDataSource;
    }

    /**
     * 处理Weaviate数据源类型的文件事件
     *
     * @param event 文件事件对象，包含事件类型为'weaviate-datasource'的相关信息
     */
    @EventListener(condition = "#event.type == 'weaviate-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            // 处理文件事件的核心逻辑
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 处理组事件的监听器方法
     * 当接收到类型为'weaviate-datasource'的事件时触发
     *
     * @param event 组事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'weaviate-datasource'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 注册Weaviate数据源信息
     *
     * @param entity Weaviate数据源信息实体对象
     * @return 注册成功返回true，否则返回false
     */
    @Override
    public boolean register(WeaviateDataSourceInfo entity) {
        // 销毁已存在的客户端连接
        magicDynamicWeaviateDataSource.destroyClientByKey(entity.getKey());
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注册Weaviate数据源映射节点
     *
     * @param mappingNode 包含Weaviate数据源信息的映射节点
     * @return 注册结果，true表示注册成功，false表示注册失败
     */
    @Override
    protected boolean register(MappingNode<WeaviateDataSourceInfo> mappingNode) {
        WeaviateDataSourceInfo entity = mappingNode.getEntity();
        // 销毁旧的客户端连接并创建新的客户端连接
        magicDynamicWeaviateDataSource.destroyClientByKey(entity.getKey());
        magicDynamicWeaviateDataSource.createClient(entity);
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注销映射节点，释放相关资源
     *
     * @param mappingNode 映射节点对象，包含Weaviate数据源信息
     */
    @Override
    protected void unregister(MappingNode<WeaviateDataSourceInfo> mappingNode) {
        // 销毁指定key的Weaviate客户端连接
        magicDynamicWeaviateDataSource.destroyClientByKey(mappingNode.getEntity().getKey());
        // 调用父类的注销方法完成剩余的注销逻辑
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }

    /**
     * 获取默认的数据源映射列表
     *
     * @return 返回Weaviate数据源信息列表，包含默认配置的数据源信息
     */
    @Override
    public List<WeaviateDataSourceInfo> defaultMappings() {
        // 筛选出ID为空的数据源节点，并转换为数据源信息对象
        return magicDynamicWeaviateDataSource.datasourceNodes().stream().filter(it -> it.getId() == null).map(it -> {
            WeaviateDataSourceInfo dataSourceInfo = new WeaviateDataSourceInfo();
            // 设置数据源名称，优先使用name字段，如果为空则使用key字段，都为空则使用"默认数据源"
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
    }

}
