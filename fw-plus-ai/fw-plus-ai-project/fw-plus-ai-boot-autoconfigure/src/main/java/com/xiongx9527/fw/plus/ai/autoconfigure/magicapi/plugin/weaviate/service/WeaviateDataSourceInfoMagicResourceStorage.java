package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.service;


import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.common.BaseService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.model.WeaviateDataSourceInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

public class WeaviateDataSourceInfoMagicResourceStorage implements MagicResourceStorage<WeaviateDataSourceInfo>, BaseService {

    /**
     * 魔法资源服务实例
     * 用于处理魔法资源相关的业务逻辑
     */
    private MagicResourceService magicResourceService;

    /**
     * 获取存储文件夹名称
     * 
     * @return 文件夹名称
     */
    @Override
    public String folder() {
        return "weaviate-datasource";
    }

    /**
     * 获取文件后缀名
     *
     * @return 返回JSON文件的后缀名 ".json"
     */
    @Override
    public String suffix() {
        return ".json";
    }

    /**
     * 返回数据源信息类的类型
     *
     * @return WeaviateDataSourceInfo类的Class对象
     */
    @Override
    public Class<WeaviateDataSourceInfo> magicClass() {
        return WeaviateDataSourceInfo. class;
    }

    /**
     * 是否需要路径
     * 
     * @return false
     */
    @Override
    public boolean requirePath() {
        return false;
    }

    /**
     * 是否需要脚本
     * 
     * @return false
     */
    @Override
    public boolean requiredScript() {
        return false;
    }

    /**
     * 是否允许根路径
     * 
     * @return true
     */
    @Override
    public boolean allowRoot() {
        return true;
    }

    /**
     * 构建映射键值
     *
     * @param info Weaviate数据源信息对象，包含构建键值所需的key和更新时间信息
     * @return 返回格式化的映射键值字符串，格式为"key-updateTime"
     */
    @Override
    public String buildMappingKey(WeaviateDataSourceInfo info) {
        // 使用key和更新时间拼接成唯一映射键值
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }

    /**
     * 验证Weaviate数据源信息的合法性
     *
     * @param entity 待验证的Weaviate数据源信息实体对象
     */
    @Override
    public void validate(WeaviateDataSourceInfo entity) {
        // 校验必要字段
        validateKeyUnique(entity.getKey(), entity.getId());
        validateField(entity);

        // 如果是更新操作，校验关键字段不允许修改
        if (entity.getId() != null) {
            WeaviateDataSourceInfo oldKafkaDataSourceInfo =  magicResourceService.file(entity.getId());
            if (null != oldKafkaDataSourceInfo) {
                if (!oldKafkaDataSourceInfo.getKey().equals(entity.getKey())) {
                    throw new IllegalArgumentException("key不可修改");
                }
            }
        }
    }

    /**
     * 验证key的唯一性
     * 
     * @param key 数据源唯一标识
     * @param currentId 当前数据源ID
     */
    private void validateKeyUnique(String key, String currentId) {
        if (key == null || magicResourceService == null) {
            return;
        }
        
        // 获取所有已存在的数据源
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .map(it -> (WeaviateDataSourceInfo)it)
                .filter(it -> !it.getId().equals(currentId))
                .noneMatch(it -> Objects.equals(it.getKey(), key));

        if (!noneMatchKey) {
            throw new IllegalArgumentException(String.format("数据源唯一标识(key) '%s' 已存在，请使用其他唯一标识", key));
        }

    }

    /**
     * 设置魔法资源服务实例
     *
     * @param magicResourceService 魔法资源服务实例
     */
    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    /**
     * 从字节数组中读取并反序列化为WeaviateDataSourceInfo对象
     *
     * @param bytes 包含序列化数据的字节数组
     * @return 反序列化后的WeaviateDataSourceInfo对象
     */
    @Override
    public WeaviateDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, WeaviateDataSourceInfo.class);
    }

    /**
     * 将MagicEntity对象序列化为字节数组
     *
     * @param entity 待序列化的MagicEntity实体对象
     * @return 序列化后的字节数组
     */
    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }


    /**
     * 读取资源并转换为Weaviate数据源信息
     *
     * @param resource 要读取的资源对象
     * @return 转换后的Weaviate数据源信息
     */
    @Override
    public WeaviateDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }

}
