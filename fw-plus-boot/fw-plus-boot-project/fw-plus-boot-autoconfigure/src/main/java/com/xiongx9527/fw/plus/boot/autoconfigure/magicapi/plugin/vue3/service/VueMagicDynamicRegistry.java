package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.vue3.model.VueInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

@Slf4j
public class VueMagicDynamicRegistry extends AbstractMagicDynamicRegistry<VueInfo> {


    /**
     * 构造函数，初始化VueMagicDynamicRegistry实例
     * @param magicResourceStorage 魔法资源存储器，用于存储VueInfo类型的资源信息
     */
    public VueMagicDynamicRegistry(MagicResourceStorage<VueInfo> magicResourceStorage) {
        super(magicResourceStorage);
    }

    /**
     * 监听并处理Vue3文件事件
     * 当接收到类型为'vue3'的文件事件时，调用processEvent方法进行处理
     * @param event 文件事件对象，包含文件相关的事件信息
     */
    @EventListener(condition = "#event.type == 'vue3'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 监听并处理Vue3组事件
     * 当接收到类型为'vue3'的组事件时，调用processEvent方法进行处理
     * @param event 组事件对象，包含组相关的事件信息
     */
    @EventListener(condition = "#event.type == 'vue3'")
    public void onGroupEvent(GroupEvent event) {
            try {
        processEvent(event);
            } catch (Exception ignored) {
            }
    }

    /**
     * 注册VueInfo实体
     * @param entity VueInfo实体对象
     * @return 注册成功返回true，否则返回false
     */
    @Override
    public boolean register(VueInfo entity) {
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注销VueInfo实体
     * @param entity VueInfo实体对象
     * @return 注销成功返回true，否则返回false
     */
    @Override
    public boolean unregister(VueInfo entity){
        try {
            return super.unregister(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注册映射节点
     * @param mappingNode VueInfo类型的映射节点
     * @return 注册成功返回true，否则返回false
     */
    @Override
    protected boolean register(MappingNode<VueInfo> mappingNode) {
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注销映射节点
     * @param mappingNode VueInfo类型的映射节点
     */
    @Override
    protected void unregister(MappingNode<VueInfo> mappingNode) {
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }

}
