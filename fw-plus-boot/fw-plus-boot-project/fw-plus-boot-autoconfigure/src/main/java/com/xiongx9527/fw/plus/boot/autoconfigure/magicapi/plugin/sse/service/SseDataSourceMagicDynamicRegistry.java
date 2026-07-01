package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.handler.BaseDynamicSseHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model.SseDataSourceInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

import java.util.ArrayList;
import java.util.List;


@Slf4j
public class SseDataSourceMagicDynamicRegistry extends AbstractMagicDynamicRegistry<SseDataSourceInfo> {

	/**
	 * 构造一个SseDataSourceMagicDynamicRegistry实例
	 *
	 * @param magicResourceStorage 魔法资源存储器，用于存储SseDataSourceInfo类型的资源信息
	 */
	public SseDataSourceMagicDynamicRegistry(MagicResourceStorage<SseDataSourceInfo> magicResourceStorage) {
		super(magicResourceStorage);
	}

	/**
	 * 处理文件事件的监听器方法
	 * 当接收到类型为'sse-datasource'的FileEvent事件时触发
	 * @param event 文件事件对象，包含事件相关信息
	 */
	@EventListener(condition = "#event.type == 'sse-datasource'")
	public void onFileEvent(FileEvent event) {
		try {
			processEvent(event);
		} catch (Exception ignored) {
        }
	}

	/**
	 * 处理组事件的监听器方法
	 * 当接收到类型为'sse-datasource'的GroupEvent事件时触发
	 * @param event 组事件对象，包含事件相关信息
	 */
	@EventListener(condition = "#event.type == 'sse-datasource'")
	public void onGroupEvent(GroupEvent event) {
		    try {
		processEvent(event);
		    } catch (Exception ignored) {
		    }
	}

	/**
	 * 注册数据源信息实体
	 * @param entity 数据源信息实体对象
	 * @return 注册成功返回true，失败返回false
	 */
	@Override
	public boolean register(SseDataSourceInfo entity) {
		try {
			return super.register(entity);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 注册映射节点
	 * @param mappingNode 映射节点对象
	 * @return 注册成功返回true，失败返回false
	 */
	@Override
	protected boolean register(MappingNode<SseDataSourceInfo> mappingNode) {
		try {
		    return super.register(mappingNode);
		} catch (Exception ignored) {
		    return false;
		}
	}

	/**
	 * 注销映射节点
	 * @param mappingNode 需要注销的映射节点对象
	 */
	@Override
	protected void unregister(MappingNode<SseDataSourceInfo> mappingNode) {
		try {
			SpringUtils.getBean(BaseDynamicSseHandler.class)
					.cleanupByAppId(mappingNode.getEntity().getKey());
		} catch (Exception e) {
			log.warn("Failed to cleanup sessions for appId: {}", mappingNode.getEntity().getKey(), e);
		}
		try {
		    super.unregister(mappingNode);
		} catch (Exception ignored) {
		}
	}


	/**
	 * 获取默认的映射列表
	 * @return 空的数据源信息列表
	 */
	@Override
	public List<SseDataSourceInfo> defaultMappings() {
		return new ArrayList<>();
	}


}
