package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.service;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
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
public class DtpDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<DtpDataSourceInfo> {

    private final MagicDynamicDtpDataSource magicDynamicDtpDataSource;

    public DtpDataSourceMagicDynamicRegistry(MagicResourceStorage<DtpDataSourceInfo> magicResourceStorage,
                                             MagicDynamicDtpDataSource magicDynamicDtpDataSource) {
        super(magicResourceStorage);
        this.magicDynamicDtpDataSource = magicDynamicDtpDataSource;
    }

    @EventListener(condition = "#event.type == 'dtp-datasource'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.warn("处理 DTP 数据源文件事件失败: {}", event, e);
        }
    }

    @EventListener(condition = "#event.type == 'dtp-datasource'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            log.warn("处理 DTP 数据源分组事件失败: {}", event, e);
        }
    }

    @Override
    public boolean register(DtpDataSourceInfo entity) {
        try {
            return super.register(entity);
        } catch (Exception e) {
            log.warn("注册 DTP 数据源失败: {}", entity == null ? null : entity.getKey(), e);
            return false;
        }
    }

    @Override
    protected boolean register(MappingNode<DtpDataSourceInfo> mappingNode) {
        DtpDataSourceInfo entity = mappingNode.getEntity();
        // 销毁旧的客户端连接并创建新的客户端连接
        magicDynamicDtpDataSource.createClient(entity);
        try {
            return super.register(mappingNode);
        } catch (Exception e) {
            log.warn("注册 DTP 数据源映射失败: {}", entity == null ? null : entity.getKey(), e);
            return false;
        }
    }

    @Override
    protected void unregister(MappingNode<DtpDataSourceInfo> mappingNode) {
        magicDynamicDtpDataSource.destroyClientByKey(mappingNode.getEntity().getKey());
        try {
            super.unregister(mappingNode);
        } catch (Exception e) {
            log.warn("注销 DTP 数据源失败: {}", mappingNode.getEntity().getKey(), e);
        }
    }

    @Override
    public List<DtpDataSourceInfo> defaultMappings() {
        return magicDynamicDtpDataSource.datasourceNodes().stream().filter(it -> it.getId() == null).map(it -> {
            DtpDataSourceInfo dataSourceInfo = new DtpDataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), I18nMessageResolver.resolveKey("fw.boot.datasource.defaultName"))));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
    }

}
