package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.MagicDynamicDtpDataSource;
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
        } catch (Exception ignored) {
        }
    }

    @EventListener(condition = "#event.type == 'dtp-datasource'")
    public void onGroupEvent(GroupEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean register(DtpDataSourceInfo entity) {
        magicDynamicDtpDataSource.destroyClientByKey(entity.getKey());
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected boolean register(MappingNode<DtpDataSourceInfo> mappingNode) {
        DtpDataSourceInfo entity = mappingNode.getEntity();
        // 销毁旧的客户端连接并创建新的客户端连接
        magicDynamicDtpDataSource.destroyClientByKey(entity.getKey());
        magicDynamicDtpDataSource.createClient(entity);
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    protected void unregister(MappingNode<DtpDataSourceInfo> mappingNode) {
        magicDynamicDtpDataSource.destroyClientByKey(mappingNode.getEntity().getKey());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }

    @Override
    public List<DtpDataSourceInfo> defaultMappings() {
        return magicDynamicDtpDataSource.datasourceNodes().stream().filter(it -> it.getId() == null).map(it -> {
            DtpDataSourceInfo dataSourceInfo = new DtpDataSourceInfo();
            dataSourceInfo.setName(StringUtils.defaultIfBlank(it.getName(), StringUtils.defaultIfBlank(it.getKey(), "默认数据源")));
            dataSourceInfo.setKey(it.getKey());
            return dataSourceInfo;
        }).collect(Collectors.toList());
    }

}
