package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.ElasticSearchDataSourceInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.MagicDynamicElasticSearchDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.List;

@Slf4j
public class ElasticsearchDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<ElasticSearchDataSourceInfo> {

    /**
     * 魔法动态ElasticSearch数据源实例
     */
    private final MagicDynamicElasticSearchDataSource magicElasticSearchDataSource;

    /**
     * Redis数据源魔法动态注册器构造函数
     *
     * @param magicResourceStorage 魔法资源存储器，用于存储ElasticSearch数据源信息
     * @param magicElasticSearchDataSource 魔法动态ElasticSearch数据源实例
     */
    public ElasticsearchDataSourceMagicDynamicRegistry(MagicResourceStorage<ElasticSearchDataSourceInfo> magicResourceStorage, MagicDynamicElasticSearchDataSource magicElasticSearchDataSource) {
        super(magicResourceStorage);
        this.magicElasticSearchDataSource = magicElasticSearchDataSource;
    }


    /**
     * 处理文件事件的监听器方法
     * 只处理类型为 'elasticsearch-datasource' 的文件事件
     *
     * @param event 文件事件对象
     */
    @EventListener(condition = "#event.type == 'elasticsearch-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 处理分组事件的监听器方法
     * 只处理类型为 'elasticsearch-datasource' 的分组事件
     *
     * @param event 分组事件对象
     */
    @EventListener(condition = "#event.type == 'elasticsearch-datasource'")
    public void onGroupEvent(GroupEvent event) {
            try {
               processEvent(event);
            } catch (Exception ignored) {
            }
    }

    /**
     * 注册ElasticSearch数据源信息
     * 在注册前先销毁已存在的同key数据源，然后调用父类注册方法
     *
     * @param entity ElasticSearch数据源信息实体
     * @return 注册是否成功
     */
    @Override
    public boolean register(ElasticSearchDataSourceInfo entity) {
        magicElasticSearchDataSource.destroyByKey(entity.getKey());
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注册映射节点中的ElasticSearch数据源
     * 销毁已存在的同key数据源，构建新的ElasticSearch REST客户端，然后调用父类注册方法
     *
     * @param mappingNode 映射节点，包含ElasticSearch数据源信息
     * @return 注册是否成功
     */
    @Override
    protected boolean register(MappingNode<ElasticSearchDataSourceInfo> mappingNode) {
        // 获取映射节点中的实体对象
        ElasticSearchDataSourceInfo entity = mappingNode.getEntity();
        // 销毁已存在的同key数据源
        magicElasticSearchDataSource.destroyByKey(entity.getKey());
        // 构建ElasticSearch REST客户端
        magicElasticSearchDataSource.buildElasticSearchRestClient(entity);
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注销映射节点中的ElasticSearch数据源
     * 销毁对应key的数据源，然后调用父类注销方法
     *
     * @param mappingNode 映射节点，包含ElasticSearch数据源信息
     */
    @Override
    protected void unregister(MappingNode<ElasticSearchDataSourceInfo> mappingNode) {
        // 销毁对应key的数据源
        magicElasticSearchDataSource.destroyByKey(mappingNode.getEntity().getKey());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }

    /**
     * 获取默认的数据源映射列表
     *
     * @return ElasticSearch数据源信息列表
     */
    @Override
    public List<ElasticSearchDataSourceInfo> defaultMappings() {
        return magicElasticSearchDataSource.datasourceNodes();
    }

}
