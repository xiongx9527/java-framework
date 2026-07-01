package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsDataSourceInfo;
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
public class WsDataSourceInfoMagicResourceStorage implements MagicResourceStorage<WsDataSourceInfo>, BaseService {

	/**
     * 魔法资源服务实例
     * 用于处理魔法资源相关的业务逻辑
     */
    private MagicResourceService magicResourceService;


	/**
	 * 获取WebSocket数据源资源存储的文件夹名称
	 *
	 * @return String 返回"ws-datasource"作为存储文件夹名称
	 */
	@Override
	public String folder() {
		return "ws-datasource";
	}

	/**
	 * 获取WebSocket数据源资源文件的后缀名
	 *
	 * @return String 返回".json"作为文件后缀名
	 */
	@Override
	public String suffix() {
		return ".json";
	}

	/**
	 * 获取WebSocket数据源信息的类类型
	 *
	 * @return Class<WsDataSourceInfo> 返回WsDataSourceInfo类的Class对象
	 */
	@Override
	public Class<WsDataSourceInfo> magicClass() {
		return WsDataSourceInfo.class;
	}

	/**
	 * 判断WebSocket数据源是否需要路径配置
	 *
	 * @return boolean 返回false，表示WebSocket数据源不需要路径配置
	 */
	@Override
	public boolean requirePath() {
		return false;
	}

	/**
	 * 判断WebSocket数据源是否需要脚本配置
	 *
	 * @return boolean 返回false，表示WebSocket数据源不需要脚本配置
	 */
	@Override
	public boolean requiredScript() {
		return false;
	}

	/**
	 * 判断WebSocket数据源是否允许在根目录下存储
	 *
	 * @return boolean 返回true，表示WebSocket数据源允许在根目录下存储
	 */
	@Override
	public boolean allowRoot() {
		return true;
	}

	/**
	 * 构建WebSocket数据源的映射键
	 * 使用数据源的key和更新时间组成唯一标识符
	 *
	 * @param info WebSocket数据源信息对象
	 * @return String 返回格式为"key-更新时间"的字符串作为映射键
	 */
	@Override
	public String buildMappingKey(WsDataSourceInfo info) {
		return String.format("%s-%s", info.getKey(), info.getUpdateTime());
	}

	/**
	 * 验证WebSocket数据源信息的合法性
	 * 检查必需字段、key的唯一性以及key的不可修改性
	 *
	 * @param entity WebSocket数据源信息对象
	 * @throws IllegalArgumentException 当验证失败时抛出异常
	 */
	@Override
	public void validate(WsDataSourceInfo entity) {
		// 验证key字段为必需字段
		validateRequiredField(entity.getKey(), "key");
		// 验证key的唯一性
		validateKeyUnique(entity.getKey(), entity.getId());
		// 验证name字段为必需字段
		validateRequiredField(entity.getName(), "name");
		// 验证secret字段为必需字段
		validateRequiredField(entity.getSecret(), "secret");
		// 验证other字段是否为JSON格式 验证other字段为JSON格式
		validateJSON(entity.getOther(), "other");
		// 如果实体已存在，验证key不可修改
		if (entity.getId() != null) {
			WsDataSourceInfo oldWsDataSourceInfo = magicResourceService.file(entity.getId());
			if (null != oldWsDataSourceInfo) {
				if (!oldWsDataSourceInfo.getKey().equals(entity.getKey())) {
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
                .map(it -> (WsDataSourceInfo)it)
                .filter(it -> !it.getId().equals(currentId))
                .noneMatch(it -> Objects.equals(it.getKey(), key));

        if (!noneMatchKey) {
            throw new IllegalArgumentException(String.format("数据源唯一标识(key) '%s' 已存在，请使用其他唯一标识", key));
        }
    }

    /**
	 * 查找所有WebSocket数据源信息
	 * 通过调用magicResourceService的files方法获取指定文件夹下的所有数据源文件
	 *
	 * @return List<WsDataSourceInfo> 返回所有WebSocket数据源信息列表
	 */
	public List<WsDataSourceInfo> findAll() {
		return magicResourceService.files(folder());
	}

	/**
	 * 设置魔法资源服务
	 *
	 * @param magicResourceService 魔法资源服务实例
	 */
	@Override
	public void setMagicResourceService(MagicResourceService magicResourceService) {
		this.magicResourceService = magicResourceService;
	}

	/**
	 * 获取魔法资源服务
	 *
	 * @return MagicResourceService 返回魔法资源服务实例
	 */
	public MagicResourceService getMagicResourceService() {
		return magicResourceService;
	}

	/**
	 * 从字节数组中读取并解析为WebSocket数据源信息对象
	 *
	 * @param bytes 字节数组
	 * @return WsDataSourceInfo WebSocket数据源信息对象
	 */
	@Override
	public WsDataSourceInfo read(byte[] bytes) {
		return JsonUtils.readValue(bytes, WsDataSourceInfo.class);
	}

	/**
	 * 将魔法实体对象序列化为字节数组
	 *
	 * @param entity 魔法实体对象
	 * @return byte[] 序列化后的字节数组
	 */
	@Override
	public byte[] write(MagicEntity entity) {
		return JsonUtils.toJsonBytes(entity);
	}

	/**
	 * 从资源中读取并解析为WebSocket数据源信息对象
	 *
	 * @param resource 资源对象
	 * @return WsDataSourceInfo WebSocket数据源信息对象
	 */
	@Override
	public WsDataSourceInfo readResource(Resource resource) {
		return read(resource.read());
	}
}
