package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.cluster.service;

import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.*;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.model.MagicNotify;
import org.ssssssss.magicapi.core.service.MagicNotifyService;

public class MagicSynchronizationService {

	/**
	 * 魔法通知服务实例
	 * 用于处理魔法相关的通知业务逻辑
	 */
	private final MagicNotifyService magicNotifyService;

	/**
	 * 当前实例ID
	 */
	private final String instanceId;

	/**
	 * 构造函数，初始化MagicSynchronizationService实例
	 * @param magicNotifyService 通知服务实例，用于发送同步通知
	 * @param instanceId 实例ID，用于标识当前实例
	 */
	public MagicSynchronizationService(MagicNotifyService magicNotifyService, String instanceId) {
		this.magicNotifyService = magicNotifyService;
		this.instanceId = instanceId;
	}

	/**
	 * 处理文件夹事件的监听器方法
	 * 当文件夹发生创建、保存、移动或删除操作时，向其他实例发送同步通知
	 * @param event 文件夹事件对象，包含事件类型和相关数据
	 */
	@EventListener(condition = "#event.source != T(org.ssssssss.magicapi.core.config.Constants).EVENT_SOURCE_NOTIFY")
	public void onFolderEvent(GroupEvent event) {
		// 根据不同的操作类型发送相应的通知
		switch (event.getAction()) {
			case CREATE:
			case SAVE:
			case MOVE:
			case DELETE:
				magicNotifyService.sendNotify(new MagicNotify(instanceId, event.getGroup().getId(), event.getAction(), event.getType()));
				break;
		}
	}

	/**
	 * 处理文件事件的监听器方法
	 * 当文件发生创建、保存、移动或删除操作时，向其他实例发送同步通知
	 * @param event 文件事件对象，包含事件类型和相关数据
	 */
	@EventListener(condition = "#event.source != T(org.ssssssss.magicapi.core.config.Constants).EVENT_SOURCE_NOTIFY")
	public void onFileEvent(FileEvent event) {
		// 过滤掉来自通知源的事件，避免重复处理
		if (Constants.EVENT_SOURCE_NOTIFY.equals(event.getSource())) {
			return;
		}
		// 根据不同的操作类型发送相应的通知
		switch (event.getAction()) {
			case CREATE:
			case SAVE:
			case MOVE:
			case DELETE:
				magicNotifyService.sendNotify(new MagicNotify(instanceId, event.getEntity().getId(), event.getAction(), Constants.EVENT_TYPE_FILE));
				break;
		}
	}

	/**
	 * 处理清除事件的监听器方法
	 * 当收到清除操作事件时，向其他实例发送清除通知
	 * @param event 魔法事件对象，此处特指清除事件
	 */
	@EventListener(condition = "#event.action == T(org.ssssssss.magicapi.core.event.EventAction).CLEAR && #event.source != T(org.ssssssss.magicapi.core.config.Constants).EVENT_SOURCE_NOTIFY")
	public void onClearEvent(MagicEvent event){
		magicNotifyService.sendNotify(new MagicNotify(instanceId, null, event.getAction(), null));
	}



}
