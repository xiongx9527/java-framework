package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.service;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.MagicDynamicS3DataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class S3DataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<S3DataSourceInfo> {

    /**
     * 动态S3数据源管理器
     * 用于动态管理和操作S3数据源的实例
     */
    private final MagicDynamicS3DataSource magicDynamicS3DataSource;

    private final S3DataSourceInfoMagicResourceStorage s3DataSourceInfoMagicResourceStorage;
    
    /**
     * 构造函数
     * 初始化S3数据源动态注册器
     *
     * @param magicResourceStorage         MagicAPI资源存储器
     * @param magicDynamicS3DataSource S3数据源管理器
     */
    public S3DataSourceMagicDynamicRegistry(S3DataSourceInfoMagicResourceStorage magicResourceStorage, MagicDynamicS3DataSource magicDynamicS3DataSource) {
        super(magicResourceStorage);
        this.s3DataSourceInfoMagicResourceStorage = magicResourceStorage;
        this.magicDynamicS3DataSource = magicDynamicS3DataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerPersistedDataSources() {
        try {
            ListUtils.emptyIfNull(s3DataSourceInfoMagicResourceStorage.findAll()).forEach(this::registerPersistedDataSource);
        } catch (Exception e) {
            log.warn("加载已保存的 S3 数据源失败", e);
        }
    }

    private void registerPersistedDataSource(S3DataSourceInfo entity) {
        if (entity == null || StringUtils.isBlank(entity.getKey())) {
            return;
        }
        try {
            if (StringUtils.isBlank(entity.getId())) {
                magicDynamicS3DataSource.destroyFileStorageServiceByKey(entity.getKey());
                magicDynamicS3DataSource.buildFileStorageService(entity);
                return;
            }
            boolean registered = register(entity);
            if (!registered) {
                log.warn("注册 S3 数据源失败: {}", entity.getKey());
            }
        } catch (Exception e) {
            log.error("注册 S3 数据源失败: {}", entity.getKey(), e);
        }
    }

    /**
     * 处理文件事件
     * 当接收到类型为"s3-datasource"的文件事件时，调用processEvent方法进行处理
     *
     * @param event 文件事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 's3-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.warn("处理 S3 数据源文件事件失败: {}", event, e);
        }
    }

    /**
     * 处理分组事件
     * 当接收到类型为"s3-datasource"的分组事件时，调用processEvent方法进行处理
     *
     * @param event 分组事件对象，包含事件类型和相关数据
     */
    @EventListener(condition = "#event.type == 's3-datasource'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.warn("处理 S3 数据源分组事件失败: {}", event, e);
        }
    }

    /**
     * 注册或注销S3数据源
     * 根据数据源的启用状态，启动或停止对应的S3监听引擎
     *
     * @param entity S3数据源信息对象
     * @return 注册是否成功
     */
    @Override
    public boolean register(S3DataSourceInfo entity) {
        magicDynamicS3DataSource.destroyFileStorageServiceByKey(entity.getKey());
        try {
            return super.register(entity);
        } catch (Exception e) {
            log.warn("注册 S3 数据源失败: {}", entity == null ? null : entity.getKey(), e);
            return false;
        }
    }

    /**
     * 注册映射节点
     * 调用父类方法注册S3数据源的映射节点
     *
     * @param mappingNode S3数据源映射节点
     * @return 注册是否成功
     */
    @Override
    protected boolean register(MappingNode<S3DataSourceInfo> mappingNode) {
        S3DataSourceInfo entity = mappingNode.getEntity();
        magicDynamicS3DataSource.destroyFileStorageServiceByKey(entity.getKey());
        magicDynamicS3DataSource.buildFileStorageService(entity);
        try {
            return super.register(mappingNode);
        } catch (Exception e) {
            log.warn("注册 S3 数据源映射失败: {}", entity == null ? null : entity.getKey(), e);
            return false;
        }
    }

    /**
     * 注销映射节点
     * 停止并销毁对应的数据源监听引擎，完成注销操作
     *
     * @param mappingNode S3数据源映射节点
     */
    @Override
    protected void unregister(MappingNode<S3DataSourceInfo> mappingNode) {
        magicDynamicS3DataSource.destroyFileStorageServiceByKey(mappingNode.getEntity().getKey());
        try {
            super.unregister(mappingNode);
        } catch (Exception e) {
            log.warn("注销 S3 数据源失败: {}", mappingNode.getEntity().getKey(), e);
        }
    }

    /**
     * 获取默认的映射列表
     * 返回S3数据源信息列表作为默认映射
     *
     * @return S3数据源信息列表
     */
    @Override
    public List<S3DataSourceInfo> defaultMappings() {
        return magicDynamicS3DataSource.datasourceNodes().stream().filter(it -> it.getId() == null).map(it -> {
            S3DataSourceInfo dataSourceInfo = new S3DataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), I18nMessageResolver.resolveKey("fw.boot.datasource.defaultName"))));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
    }
}
