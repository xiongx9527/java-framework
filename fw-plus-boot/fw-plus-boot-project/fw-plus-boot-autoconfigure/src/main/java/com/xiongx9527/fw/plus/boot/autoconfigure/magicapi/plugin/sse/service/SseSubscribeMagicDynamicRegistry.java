package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model.SseSubscribeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;


@Slf4j
public class SseSubscribeMagicDynamicRegistry extends AbstractMagicDynamicRegistry<SseSubscribeInfo> {

	/**
	 * 构造函数，用于创建SseSubscribeMagicDynamicRegistry实例
	 *
	 * @param magicResourceStorage 魔法资源存储器，用于存储SseSubscribeInfo类型的资源信息
	 */
	public SseSubscribeMagicDynamicRegistry(MagicResourceStorage<SseSubscribeInfo> magicResourceStorage) {
		// 调用父类构造函数初始化魔法资源存储
		super(magicResourceStorage);
	}


	/**
	 * 处理文件事件的监听方法
	 * 当接收到类型为'sse-subscribe'的文件事件时，会触发此方法进行事件处理
	 *
	 * @param event 文件事件对象，包含事件类型和其他相关数据
	 */
	@EventListener(condition = "#event.type == 'sse-subscribe'")
	public void onFileEvent(FileEvent event) {
		try {
			// 处理文件事件的核心逻辑
			processEvent(event);
		} catch (Exception ignored) {
        }
	}

	/**
	 * 处理组事件的监听方法，当接收到类型为'sse-subscribe'的事件时触发
	 * @param event 组事件对象，包含事件类型和相关数据
	 */
	@EventListener(condition = "#event.type == 'sse-subscribe'")
	public void onGroupEvent(GroupEvent event) {
		    try {
		processEvent(event);
		    } catch (Exception ignored) {
		    }
	}

	/**
	 * 注册SSE订阅信息
	 *
	 * @param entity SSE订阅信息实体对象
	 * @return 注册成功返回true，注册失败返回false
	 */
	@Override
	public boolean register(SseSubscribeInfo entity) {
		try {
			// 调用父类的注册方法进行实际注册操作
			return super.register(entity);
		} catch (Exception ignored) {
			return false;
		}
	}

	/**
	 * 注册映射节点到当前处理器中
	 *
	 * @param mappingNode 包含SseSubscribeInfo信息的映射节点，用于建立订阅关系的映射
	 * @return boolean 注册成功返回true，注册失败返回false
	 */
	@Override
	protected boolean register(MappingNode<SseSubscribeInfo> mappingNode) {
		// 调用父类的注册方法完成实际的注册逻辑
		try {
		    return super.register(mappingNode);
		} catch (Exception ignored) {
		    return false;
		}
	}

	/**
	 * 注销指定的映射节点
	 *
	 * @param mappingNode 要注销的映射节点，包含SseSubscribeInfo类型的数据
	 */
	@Override
	protected void unregister(MappingNode<SseSubscribeInfo> mappingNode) {
		// 调用父类的注销方法完成实际的注销操作
		try {
		    super.unregister(mappingNode);
		} catch (Exception ignored) {
		}
	}
}
