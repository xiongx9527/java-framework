package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsSubscribeInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zzy
 */
public class WsSubscribeInfoMagicResourceStorage extends AbstractPathMagicResourceStorage<WsSubscribeInfo> implements BaseService  {

	/**
	 * 获取WebSocket订阅资源存储的文件夹名称
	 *
	 * @return String 返回"ws-subscribe"作为存储文件夹名称
	 */
	@Override
	public String folder() {
		return "ws-subscribe";
	}

	/**
	 * 判断WebSocket订阅是否需要路径配置
	 *
	 * @return boolean 返回false，表示WebSocket订阅不需要路径配置
	 */
	@Override
	public boolean requirePath() {
		return false;
	}

	/**
	 * 获取WebSocket订阅信息的类类型
	 *
	 * @return Class<WsSubscribeInfo> 返回WsSubscribeInfo类的Class对象
	 */
	@Override
	public Class<WsSubscribeInfo> magicClass() {
		return WsSubscribeInfo.class;
	}

	/**
	 * 验证WebSocket订阅信息的合法性
	 * 检查必需字段、业务标识的唯一性以及数据源key的存在性
	 *
	 * @param entity WebSocket订阅信息对象
	 * @throws IllegalArgumentException 当验证失败时抛出异常
	 */
	@Override
	public void validate(WsSubscribeInfo entity) {
		// 验证key字段为必需字段
		validateRequiredField(entity.getKey(), "key");
		// 验证name字段为必需字段
		validateRequiredField(entity.getName(), "name");
		// 验证businessId字段为必需字段
		validateRequiredField(entity.getBusinessId(), "业务标识");
		// 按key分组收集所有WebSocket订阅信息
		Map<String, List<WsSubscribeInfo>> map = ListUtils.emptyIfNull(findAll()).stream().collect(Collectors.groupingBy(WsSubscribeInfo::getKey));
		// 检查同一key下业务标识的唯一性
		if (map.containsKey(entity.getKey())) {
			Map<String, WsSubscribeInfo> kafkaDataSourceInfoMap = ListUtils.emptyIfNull(map.get(entity.getKey())).stream().collect(Collectors.toMap(WsSubscribeInfo::getBusinessId, val -> val, (k1, k2) -> k1));
			if (kafkaDataSourceInfoMap.containsKey(entity.getBusinessId()) && !kafkaDataSourceInfoMap.get(entity.getBusinessId()).getId().equals(entity.getId())) {
				throw new IllegalArgumentException(String.format("业务标识:%s 已存在", entity.getBusinessId()));
			}
		}
		// 验证数据源key是否存在
		WsDataSourceInfoMagicResourceStorage kafkaDataSourceInfoMagicResourceStorage = SpringUtils.getBean(WsDataSourceInfoMagicResourceStorage.class);
		Map<String, WsDataSourceInfo> kafkaDataSourceInfoMap = ListUtils.emptyIfNull(kafkaDataSourceInfoMagicResourceStorage.findAll()).stream().collect(Collectors.toMap(WsDataSourceInfo::getKey, val -> val, (k1, k2) -> k1));
		if (!kafkaDataSourceInfoMap.containsKey(entity.getKey())) {
			throw new IllegalArgumentException(String.format("数据源key:%s 不存在", entity.getKey()));
		}

	}

	/**
	 * 查找所有WebSocket订阅信息
	 * 通过调用magicResourceService的files方法获取指定文件夹下的所有订阅文件
	 *
	 * @return List<WsSubscribeInfo> 返回所有WebSocket订阅信息列表
	 */
	public List<WsSubscribeInfo> findAll() {
		return magicResourceService.files(folder());
	}

	/**
	 * 构建WebSocket订阅信息的映射键
	 * 基于订阅信息和分组路径构建唯一的映射键
	 *
	 * @param info WebSocket订阅信息对象
	 * @return String 返回构建的映射键
	 */
	@Override
	public String buildMappingKey(WsSubscribeInfo info) {
		return buildMappingKey(info, magicResourceService.getGroupPath(info.getGroupId()));
	}
}
