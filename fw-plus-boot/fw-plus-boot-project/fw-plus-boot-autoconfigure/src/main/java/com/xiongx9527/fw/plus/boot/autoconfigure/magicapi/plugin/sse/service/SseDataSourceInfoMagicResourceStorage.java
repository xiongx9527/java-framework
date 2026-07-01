package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model.SseDataSourceInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author zzy
 */
public class SseDataSourceInfoMagicResourceStorage implements MagicResourceStorage<SseDataSourceInfo>, BaseService {

    /**
     * 魔法资源服务实例
     * 用于处理魔法资源相关的业务逻辑
     */
    private MagicResourceService magicResourceService;


	/**
	 * 获取文件夹名称
	 * @return 返回文件夹名称"sse-datasource"
	 */
	@Override
	public String folder() {
		return "sse-datasource";
	}

	/**
	 * 获取文件后缀名
	 * @return 返回文件后缀名".json"
	 */
	@Override
	public String suffix() {
		return ".json";
	}

	/**
	 * 获取魔法类的类型
	 * @return 返回SseDataSourceInfo类的Class对象
	 */
	@Override
	public Class<SseDataSourceInfo> magicClass() {
		return SseDataSourceInfo.class;
	}

	/**
	 * 判断是否需要路径
	 * @return 返回false，表示不需要路径
	 */
	@Override
	public boolean requirePath() {
		return false;
	}

	/**
	 * 判断是否需要脚本
	 * @return 返回false，表示不需要脚本
	 */
	@Override
	public boolean requiredScript() {
		return false;
	}

	/**
	 * 判断是否允许根路径
	 * @return 返回true，表示允许根路径
	 */
	@Override
	public boolean allowRoot() {
		return true;
	}

	/**
	 * 构建映射键
	 * @param info SseDataSourceInfo对象，包含数据源信息
	 * @return 返回格式化后的映射键，格式为"key-updateTime"
	 */
	@Override
	public String buildMappingKey(SseDataSourceInfo info) {
		return String.format("%s-%s", info.getKey(), info.getUpdateTime());
	}

	/**
	 * 验证SseDataSourceInfo实体的合法性
	 * @param entity 待验证的数据源信息实体
	 */
	@Override
	public void validate(SseDataSourceInfo entity) {
		// 验证必需字段
		validateRequiredField(entity.getKey(), "key");
		// 验证key的唯一性
		validateKeyUnique(entity.getKey(), entity.getId());
		validateRequiredField(entity.getName(), "name");
		validateRequiredField(entity.getSecret(), "secret");
         // 验证other字段是否为JSON格式 验证other字段为JSON格式
		validateJSON(entity.getOther(), "other");
		// 如果实体已存在，验证key不可修改
		if (entity.getId() != null) {
			SseDataSourceInfo oldDataSourceInfo = magicResourceService.file(entity.getId());
			if (null != oldDataSourceInfo) {
				if (!oldDataSourceInfo.getKey().equals(entity.getKey())) {
					throw new IllegalArgumentException("key不可修改");
				}
			}
		}
	}


    /**
     * 校验数据源key的唯一性
     * @param key 数据源key
     * @param currentId 当前数据源ID，新增时为null
     */
    private void validateKeyUnique(String key, String currentId) {
        if (key == null || magicResourceService == null) {
            return;
        }
        // 获取所有已存在的数据源
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .map(it -> (SseDataSourceInfo)it)
                .filter(it -> !it.getId().equals(currentId))
                .noneMatch(it -> Objects.equals(it.getKey(), key));

        if (!noneMatchKey) {
            throw new IllegalArgumentException(String.format("数据源唯一标识(key) '%s' 已存在，请使用其他唯一标识", key));
        }
    }


	/**
	 * 查找所有数据源信息
	 * @return 数据源信息列表
	 */
	public List<SseDataSourceInfo> findAll() {
		return magicResourceService.files(folder());
	}

	/**
	 * 设置魔术资源服务
	 * @param magicResourceService 魔术资源服务实例
	 */
	@Override
	public void setMagicResourceService(MagicResourceService magicResourceService) {
		this.magicResourceService = magicResourceService;
	}

	/**
	 * 获取魔术资源服务
	 * @return 魔术资源服务实例
	 */
	public MagicResourceService getMagicResourceService() {
		return magicResourceService;
	}

	/**
	 * 从字节数组读取数据源信息
	 * @param bytes 字节数组
	 * @return 数据源信息对象
	 */
	@Override
	public SseDataSourceInfo read(byte[] bytes) {
		return JsonUtils.readValue(bytes, SseDataSourceInfo.class);
	}

	/**
	 * 将实体对象写入字节数组
	 * @param entity 魔术实体对象
	 * @return 字节数组
	 */
	@Override
	public byte[] write(MagicEntity entity) {
		return JsonUtils.toJsonBytes(entity);
	}

	/**
	 * 从资源对象读取数据源信息
	 * @param resource 资源对象
	 * @return 数据源信息对象
	 */
	@Override
	public SseDataSourceInfo readResource(Resource resource) {
		return read(resource.read());
	}


}
