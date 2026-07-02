package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsSubscribeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;


@Slf4j
public class WsSubscribeMagicDynamicRegistry extends AbstractMagicDynamicRegistry<WsSubscribeInfo> {

	/**
	 * 构造函数，初始化WebSocket订阅动态注册器
	 *
	 * @param magicResourceStorage WebSocket订阅信息存储器
	 */
	public WsSubscribeMagicDynamicRegistry(MagicResourceStorage<WsSubscribeInfo> magicResourceStorage) {
		// 调用父类构造函数初始化魔法资源存储
		super(magicResourceStorage);
	}


	/**
	 * 处理WebSocket订阅相关的文件事件
	 * 当监听到类型为'ws-subscribe'的FileEvent事件时，调用processEvent方法进行处理
	 * 并捕获可能发生的异常，记录错误日志
	 *
	 * @param event 文件事件对象，包含事件类型和相关数据
	 */
	@EventListener(condition = "#event.type == 'ws-subscribe'")
	public void onFileEvent(FileEvent event) {
		try {
			processEvent(event);
		} catch (Exception e) {
			log.error("处理文件事件时发生错误: {}", e.getMessage(), e);
		}
	}


	/**
	 * 处理WebSocket订阅相关的分组事件
	 * 当监听到类型为'ws-subscribe'的GroupEvent事件时，调用processEvent方法进行处理
	 *
	 * @param event 分组事件对象，包含事件类型和相关数据
	 */
	@EventListener(condition = "#event.type == 'ws-subscribe'")
	public void onGroupEvent(GroupEvent event) {
		processEvent(event);
	}


    /**
     * 注册WebSocket订阅信息实体
     * 调用父类的注册方法来处理WebSocket订阅信息的注册
     *
     * @param entity WebSocket订阅信息实体
     * @return boolean 注册是否成功
     */
    @Override
    public boolean register(WsSubscribeInfo entity) {
		try {
			return super.register(entity);
		} catch (Exception e) {
			return false;
		}
    }


    /**
     * 注册WebSocket订阅映射节点
     * 调用父类的注册方法来处理WebSocket订阅信息的注册
     *
     * @param mappingNode WebSocket订阅信息的映射节点
     * @return boolean 注册是否成功
     */
    @Override
    protected boolean register(MappingNode<WsSubscribeInfo> mappingNode) {
        return super.register(mappingNode);
    }

	/**
	 * 注销WebSocket订阅映射节点
	 * 调用父类的注销方法来处理WebSocket订阅信息的注销
	 *
	 * @param mappingNode WebSocket订阅信息的映射节点
	 */
	@Override
	protected void unregister(MappingNode<WsSubscribeInfo> mappingNode) {
		super.unregister(mappingNode);
	}
}
