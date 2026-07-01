package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler.BaseDynamicWebSocketHandler;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsDataSourceInfo;
import com.xx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Ws 数据源 动态注册器
 *
 * @author zzy
 */
@Slf4j
public class WsDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<WsDataSourceInfo> {

	/**
	 * 构造函数，初始化WebSocket数据源动态注册器
	 *
	 * @param magicResourceStorage WebSocket数据源信息存储器
	 */
	public WsDataSourceMagicDynamicRegistry(MagicResourceStorage<WsDataSourceInfo> magicResourceStorage) {
		super(magicResourceStorage);
	}
    
	/**
	 * 处理WebSocket数据源相关的文件事件
	 * 当监听到类型为'ws-datasource'的FileEvent事件时，调用processEvent方法进行处理
	 * 并捕获可能发生的异常，记录错误日志
	 *
	 * @param event 文件事件对象，包含事件类型和相关数据
	 */
	@EventListener(condition = "#event.type == 'ws-datasource'")
	public void onFileEvent(FileEvent event) {
		try {
			processEvent(event);
		} catch (Exception ignored) {
        }
	}
    
    /**
	 * 处理WebSocket数据源相关的分组事件
	 * 当监听到类型为'ws-datasource'的GroupEvent事件时，调用processEvent方法进行处理
	 *
	 * @param event 分组事件对象，包含事件类型和相关数据
	 */
	@EventListener(condition = "#event.type == 'ws-datasource'")
	public void onGroupEvent(GroupEvent event) {
		    try {
		       processEvent(event);
		    } catch (Exception ignored) {
		    }
	}


    /**
	 * 注册WebSocket数据源信息实体
	 * 调用父类的注册方法来处理WebSocket数据源信息的注册，并捕获可能发生的异常
	 *
	 * @param entity WebSocket数据源信息实体
	 * @return boolean 注册是否成功，如果发生异常则返回false
	 */
	@Override
	public boolean register(WsDataSourceInfo entity) {
		try {
			return super.register(entity);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean unregister(WsDataSourceInfo entity) {
		try {
			return super.unregister(entity);
		} catch (Exception ignored) {
			return false;
		}
	}
    
    /**
	 * 注册WebSocket数据源映射节点
	 * 调用父类的注册方法来处理WebSocket数据源信息的注册
	 *
	 * @param mappingNode WebSocket数据源信息的映射节点
	 * @return boolean 注册是否成功
	 */
	@Override
	protected boolean register(MappingNode<WsDataSourceInfo> mappingNode) {
		try {
		    return super.register(mappingNode);
		} catch (Exception ignored) {
		    return false;
		}
	}
    
    /**
	 * 注销WebSocket数据源映射节点
	 * 调用父类的注销方法来处理WebSocket数据源信息的注销
	 *
	 * @param mappingNode WebSocket数据源信息的映射节点
	 */
	@Override
	protected void unregister(MappingNode<WsDataSourceInfo> mappingNode) {
		try {
			SpringUtils.getBean(BaseDynamicWebSocketHandler.class)
					.cleanupByAppId(mappingNode.getEntity().getKey());
		} catch (Exception e) {
			log.error("Failed to cleanup sessions for appId: {}", mappingNode.getEntity().getKey(), e);
		}
		try {
		    super.unregister(mappingNode);
		} catch (Exception ignored) {
		}
	}

    /**
	 * 获取默认的WebSocket数据源信息列表
	 * 返回一个空的ArrayList实例
	 *
	 * @return List<WsDataSourceInfo> 空的WebSocket数据源信息列表
	 */
	@Override
	public List<WsDataSourceInfo> defaultMappings() {
		return new ArrayList<>();
	}

}
