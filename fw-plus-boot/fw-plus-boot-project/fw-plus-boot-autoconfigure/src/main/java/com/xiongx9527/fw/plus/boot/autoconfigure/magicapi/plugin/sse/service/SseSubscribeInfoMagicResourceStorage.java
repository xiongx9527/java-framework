package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model.SseDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model.SseSubscribeInfo;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.apache.commons.collections4.ListUtils;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author zzy
 */
public class SseSubscribeInfoMagicResourceStorage extends AbstractPathMagicResourceStorage<SseSubscribeInfo> implements BaseService {
	/**
	 * 获取资源存储的文件夹名称
	 *
	 * @return 返回固定字符串 "sse-subscribe"，表示该资源存储在该文件夹下
	 */
	@Override
	public String folder() {
		return "sse-subscribe";
	}

	/**
	 * 判断该资源是否需要路径信息
	 *
	 * @return 返回 false，表示该资源不需要额外的路径信息
	 */
	@Override
	public boolean requirePath() {
		return false;
	}

	/**
	 * 获取资源对应的实体类类型
	 *
	 * @return 返回 SseSubscribeInfo 类的 Class 对象
	 */
	@Override
	public Class<SseSubscribeInfo> magicClass() {
		return SseSubscribeInfo.class;
	}

	/**
	 * 对 SseSubscribeInfo 实体进行校验
	 *
	 * 校验内容包括：
	 * 1. key、name、businessId 字段为必填项；
	 * 2. 在相同 key 下，businessId 必须唯一；
	 * 3. 数据源中必须存在与实体 key 对应的记录。
	 *
	 * @param entity 待校验的 SseSubscribeInfo 实体对象
	 * @throws IllegalArgumentException 当校验不通过时抛出异常，包含具体错误信息
	 */
	@Override
	public void validate(SseSubscribeInfo entity) {
		// 验证key字段为必需字段
		validateRequiredField(entity.getKey(), "key");
		// 验证name字段为必需字段
		validateRequiredField(entity.getName(), "name");
		// 验证businessId字段为必需字段
		validateRequiredField(entity.getBusinessId(), "业务标识");

		// 按 key 分组所有已存在的订阅信息，用于后续唯一性校验
		Map<String, List<SseSubscribeInfo>> map = ListUtils.emptyIfNull(findAll()).stream().collect(Collectors.groupingBy(SseSubscribeInfo::getKey));

		// 检查同一key下业务标识的唯一性
		if (map.containsKey(entity.getKey())) {
			Map<String, SseSubscribeInfo> subscribeInfoMap = ListUtils.emptyIfNull(map.get(entity.getKey())).stream().collect(Collectors.toMap(SseSubscribeInfo::getBusinessId, val -> val, (k1, k2) -> k1));
			if (subscribeInfoMap.containsKey(entity.getBusinessId()) && !subscribeInfoMap.get(entity.getBusinessId()).getId().equals(entity.getId())) {
				throw new IllegalArgumentException(String.format("业务标识:%s 已存在", entity.getBusinessId()));
			}
		}

		// 验证数据源key是否存在
		SseDataSourceInfoMagicResourceStorage dataSourceInfoStorage = SpringUtils.getBean(SseDataSourceInfoMagicResourceStorage.class);
		Map<String, SseDataSourceInfo> dataSourceInfoMap = ListUtils.emptyIfNull(dataSourceInfoStorage.findAll()).stream().collect(Collectors.toMap(SseDataSourceInfo::getKey, val -> val, (k1, k2) -> k1));
		if (!dataSourceInfoMap.containsKey(entity.getKey())) {
			throw new IllegalArgumentException(String.format("数据源key:%s 不存在", entity.getKey()));
		}

	}


	/**
	 * 查找所有SSE订阅信息
	 *
	 * @return 返回所有SSE订阅信息的列表
	 */
	public List<SseSubscribeInfo> findAll() {
		return magicResourceService.files(folder());
	}

	/**
	 * 构建映射键
	 *
	 * @param info SSE订阅信息对象
	 * @return 返回构建的映射键字符串
	 */
	@Override
	public String buildMappingKey(SseSubscribeInfo info) {
		return buildMappingKey(info, magicResourceService.getGroupPath(info.getGroupId()));
	}

}
