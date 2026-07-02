package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jDataSourceInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jDataSource;
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
public class Langchain4jDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<Langchain4jDataSourceInfo> {

    /**
     * MagicDynamicLangchain4jDataSource实例，用于提供动态数据源支持
     * 该数据源基于Langchain4j框架实现，支持魔法动态数据处理能力
     */
    private final MagicDynamicLangchain4jDataSource magicLangchain4jDataSource;

    /**
     * 构造函数，初始化Langchain4j数据源动态注册器
     *
     * @param magicResourceStorage 资源存储器，用于存储Langchain4j数据源信息
     * @param magicDynamicLangchain4jDataSource 动态Langchain4j数据源实例
     */
    public Langchain4jDataSourceMagicDynamicRegistry(MagicResourceStorage<Langchain4jDataSourceInfo> magicResourceStorage, MagicDynamicLangchain4jDataSource magicDynamicLangchain4jDataSource) {
        super(magicResourceStorage);
        this.magicLangchain4jDataSource = magicDynamicLangchain4jDataSource;
    }


   /**
     * 处理文件事件的监听方法
     * 当接收到类型为'langchain4j-datasource'的文件事件时触发此方法
     *
     * @param event 文件事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'langchain4j-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            // 处理文件事件的核心逻辑
            processEvent(event);
        } catch (Exception e) {
            // 记录处理文件事件时发生的异常信息
            log.error("处理文件事件时发生错误: {}", e.getMessage(), e);
        }
    }


    /**
     * 处理组事件的监听方法，当事件类型为'langchain4j-datasource'时触发
     *
     * @param event 组事件对象，包含事件类型和其他相关信息
     */
    @EventListener(condition = "#event.type == 'langchain4j-datasource'")
    public void onGroupEvent(GroupEvent event) {
        processEvent(event);
    }


    /**
     * 注册数据源信息
     *
     * @param entity 数据源信息实体对象
     * @return 注册成功返回true，否则返回false
     */
    @Override
    public boolean register(Langchain4jDataSourceInfo entity) {
        // 销毁旧的数据源模型
        magicLangchain4jDataSource.destroyModelByKey(entity.getKey());
        return super.register(entity);
    }


    /**
     * 注册数据源映射节点
     *
     * @param mappingNode 数据源信息映射节点，包含要注册的数据源实体信息
     * @return 注册结果，true表示注册成功，false表示注册失败
     */
    @Override
    protected boolean register(MappingNode<Langchain4jDataSourceInfo> mappingNode) {
        Langchain4jDataSourceInfo entity = mappingNode.getEntity();
        // 销毁旧的数据源模型客户端
        magicLangchain4jDataSource.destroyModelByKey(entity.getKey());
        // 创建新的数据源客户端
        magicLangchain4jDataSource.createClient(entity);
        return super.register(mappingNode);
    }


    /**
     * 注销映射节点，释放相关资源
     *
     * @param mappingNode 映射节点对象，包含Langchain4j数据源信息
     */
    @Override
    protected void unregister(MappingNode<Langchain4jDataSourceInfo> mappingNode) {
        // 销毁模型实例
        magicLangchain4jDataSource.destroyModelByKey(mappingNode.getEntity().getKey());
        // 调用父类注销方法
        super.unregister(mappingNode);
    }


    /**
     * 获取默认的数据源映射信息列表
     *
     * @return 默认数据源信息列表，包含名称和键值
     */
    @Override
    public List<Langchain4jDataSourceInfo> defaultMappings() {
        // 筛选出ID为空的数据源节点，并转换为数据源信息对象
        return magicLangchain4jDataSource.datasourceNodes().stream().filter(it -> it.getId() == null).map(it -> {
            Langchain4jDataSourceInfo dataSourceInfo = new Langchain4jDataSourceInfo();
            // 设置数据源名称，优先使用name，其次使用key，最后使用默认值"默认数据源"
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
    }

}
