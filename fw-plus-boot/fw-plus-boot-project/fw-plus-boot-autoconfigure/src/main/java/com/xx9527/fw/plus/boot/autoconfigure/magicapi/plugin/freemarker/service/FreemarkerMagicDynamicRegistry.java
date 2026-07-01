package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.model.FtlInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.freemarker.modules.FreemarkerModule;
import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

@Slf4j
public class FreemarkerMagicDynamicRegistry extends AbstractMagicDynamicRegistry<FtlInfo> {
    /**
     * 构造函数，初始化FreemarkerMagicDynamicRegistry实例
     * @param magicResourceStorage 魔法资源存储器，用于存储FtlInfo类型的资源信息
     */
    public FreemarkerMagicDynamicRegistry(MagicResourceStorage<FtlInfo> magicResourceStorage) {
        super(magicResourceStorage);
    }

    /**
     * 监听并处理文件事件，当事件类型为freemarker时触发
     * 该方法会调用processEvent处理具体的文件事件逻辑
     * @param event 文件事件对象，包含文件变化的相关信息
     */
    @EventListener(condition = "#event.type == 'freemarker'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 监听并处理组事件，当事件类型为freemarker时触发
     * 该方法会调用processEvent处理具体的组事件逻辑
     * @param event 组事件对象，包含组变化的相关信息
     */
    @EventListener(condition = "#event.type == 'freemarker'")
    public void onGroupEvent(GroupEvent event) {
            try {
        processEvent(event);
            } catch (Exception ignored) {
            }
    }

    /**
     * 清除指定实体的缓存
     *
     * @param entity FtlInfo实体对象，用于构建缓存映射键并清除对应缓存
     */
    private void clearCache(FtlInfo entity) {
        if (entity != null) {
            try {
                // 构建缓存映射键
                String mappingKey = getMagicResourceStorage().buildMappingKey(entity);
                if (mappingKey != null) {
                    // 根据映射键清除Freemarker模块的缓存
                    SpringUtils.getBean(FreemarkerModule.class).clearCache(mappingKey);
                }
            } catch (Exception e) {
                log.warn("清除缓存时发生错误: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 注册FtlInfo实体到注册中心
     * @param entity FtlInfo实体对象，包含Freemarker模板的相关信息
     * @return 注册成功返回true，否则返回false
     */
    @Override
    public boolean register(FtlInfo entity) {
        clearCache(entity);
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注册映射节点到注册中心
     * @param mappingNode 映射节点对象，包含FtlInfo实体的映射关系
     * @return 注册成功返回true，否则返回false
     */
    @Override
    protected boolean register(MappingNode<FtlInfo> mappingNode) {
        clearCache(mappingNode.getEntity());
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 从注册中心注销指定的映射节点
     * @param mappingNode 需要注销的映射节点对象
     */
    @Override
    protected void unregister(MappingNode<FtlInfo> mappingNode) {
        clearCache(mappingNode.getEntity());
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }

}
