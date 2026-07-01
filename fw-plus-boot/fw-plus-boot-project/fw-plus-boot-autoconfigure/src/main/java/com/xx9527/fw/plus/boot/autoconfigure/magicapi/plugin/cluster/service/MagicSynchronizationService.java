package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.service;

import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.*;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.model.MagicNotify;
import org.ssssssss.magicapi.core.service.MagicNotifyService;

import java.util.EnumSet;

/**
 * MagicAPI集群同步服务
 * 监听本地事件并发送集群通知，实现多实例间的数据同步
 */
public class MagicSynchronizationService {

    /**
     * 定义需要触发同步的写操作类型集合
     */
    private static final EnumSet<EventAction> WRITE_ACTIONS = EnumSet.of(
            EventAction.CREATE, EventAction.SAVE, EventAction.MOVE, EventAction.DELETE);

    /**
     * MagicAPI通知服务
     */
    private final MagicNotifyService magicNotifyService;
    
    /**
     * 当前实例ID
     */
    private final String instanceId;

    /**
     * 构造函数，初始化集群同步服务
     *
     * @param magicNotifyService 通知服务实例，用于发送集群通知
     * @param instanceId         当前实例的唯一标识
     */
    public MagicSynchronizationService(MagicNotifyService magicNotifyService, String instanceId) {
        this.magicNotifyService = magicNotifyService;
        this.instanceId = instanceId;
    }

    /**
     * 处理文件夹（分组）变更事件
     * 当检测到创建、保存、移动或删除操作时，向集群发送通知
     * 忽略来自其他节点的通知事件，避免循环传播
     *
     * @param event 文件夹事件对象，包含操作类型、分组信息等
     */
    @EventListener(condition = "#event.source != T(org.ssssssss.magicapi.core.config.Constants).EVENT_SOURCE_NOTIFY")
    public void onFolderEvent(GroupEvent event) {
        if (WRITE_ACTIONS.contains(event.getAction())) {
            magicNotifyService.sendNotify(
                    new MagicNotify(instanceId, event.getGroup().getId(), event.getAction(), event.getType()));
        }
    }

    /**
     * 处理文件变更事件
     * 当检测到创建、保存、移动或删除操作时，向集群发送通知
     * 忽略来自其他节点的通知事件，避免循环传播
     *
     * @param event 文件事件对象，包含操作类型、文件实体信息等
     */
    @EventListener(condition = "#event.source != T(org.ssssssss.magicapi.core.config.Constants).EVENT_SOURCE_NOTIFY")
    public void onFileEvent(FileEvent event) {
        if (WRITE_ACTIONS.contains(event.getAction())) {
            magicNotifyService.sendNotify(
                    new MagicNotify(instanceId, event.getEntity().getId(), event.getAction(), Constants.EVENT_TYPE_FILE));
        }
    }

    /**
     * 处理清空事件
     * 当检测到清空操作时，向集群发送通知
     * 忽略来自其他节点的通知事件，避免循环传播
     *
     * @param event Magic事件对象，包含操作类型等信息
     */
    @EventListener(condition = "#event.action == T(org.ssssssss.magicapi.core.event.EventAction).CLEAR"
            + " && #event.source != T(org.ssssssss.magicapi.core.config.Constants).EVENT_SOURCE_NOTIFY")
    public void onClearEvent(MagicEvent event) {
        magicNotifyService.sendNotify(new MagicNotify(instanceId, null, event.getAction(), null));
    }

}
