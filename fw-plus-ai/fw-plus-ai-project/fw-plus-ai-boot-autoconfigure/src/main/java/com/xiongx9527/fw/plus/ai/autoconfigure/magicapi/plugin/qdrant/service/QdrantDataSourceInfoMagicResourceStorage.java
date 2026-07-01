package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.service;


import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.common.BaseService;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.model.QdrantDataSourceInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

public class QdrantDataSourceInfoMagicResourceStorage implements MagicResourceStorage<QdrantDataSourceInfo>, BaseService {

    /**
     * 资源服务实例
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
        return "qdrant-datasource";
    }

    /**
     * 获取文件后缀名
     * 
     * @return 文件后缀
     */
    @Override
    public String suffix() {
        return ".json";
    }

    /**
     * 获取实体类类型
     * 
     * @return Qdrant数据源信息类
     */
    @Override
    public Class<QdrantDataSourceInfo> magicClass() {
        return QdrantDataSourceInfo. class;
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
     * 构建映射键
     * 
     * @param info Qdrant数据源信息
     * @return 映射键字符串
     */
    @Override
    public String buildMappingKey(QdrantDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }

    /**
     * 验证Qdrant数据源信息
     * 
     * @param entity Qdrant数据源信息实体
     */
    @Override
    public void validate(QdrantDataSourceInfo entity) {
        // 校验必要字段
        validateKeyUnique(entity.getKey(), entity.getId());
        validateField(entity);

        if (entity.getId() != null) {
            QdrantDataSourceInfo oldKafkaDataSourceInfo =  magicResourceService.file(entity.getId());
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
                .map(it -> (QdrantDataSourceInfo)it)
                .filter(it -> !it.getId().equals(currentId))
                .noneMatch(it -> Objects.equals(it.getKey(), key));

        if (!noneMatchKey) {
            throw new IllegalArgumentException(String.format("数据源唯一标识(key) '%s' 已存在，请使用其他唯一标识", key));
        }

    }

    /**
     * 设置资源服务
     * 
     * @param magicResourceService 资源服务实例
     */
    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    /**
     * 读取字节数组为Qdrant数据源信息对象
     * 
     * @param bytes 字节数组
     * @return Qdrant数据源信息对象
     */
    @Override
    public QdrantDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, QdrantDataSourceInfo.class);
    }

    /**
     * 将实体对象写入字节数组
     * 
     * @param entity 魔法实体对象
     * @return 字节数组
     */
    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    /**
     * 从资源中读取Qdrant数据源信息
     * 
     * @param resource 资源对象
     * @return Qdrant数据源信息对象
     */
    @Override
    public QdrantDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }
}
