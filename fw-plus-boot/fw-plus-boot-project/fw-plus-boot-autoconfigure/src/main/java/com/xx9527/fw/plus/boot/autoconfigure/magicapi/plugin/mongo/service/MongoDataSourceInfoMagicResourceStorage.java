package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.service;


import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MongoDataSourceInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

public class MongoDataSourceInfoMagicResourceStorage implements MagicResourceStorage<MongoDataSourceInfo>, BaseService {

    /**
     * 资源服务实例
     * 用于处理魔法资源相关的业务逻辑
     */
    private MagicResourceService magicResourceService;


    /**
     * 获取数据源配置文件夹名称
     *
     * @return 返回MongoDB数据源配置文件夹名称"mongo-datasource"
     */
    @Override
    public String folder() {
        return "mongo-datasource";
    }

    /**
     * 获取数据源配置文件后缀名
     *
     * @return 返回JSON格式配置文件的后缀名".json"
     */
    @Override
    public String suffix() {
        return ".json";
    }

    /**
     * 获取数据源信息对应的Class类型
     *
     * @return 返回MongoDataSourceInfo类的Class对象
     */
    @Override
    public Class<MongoDataSourceInfo> magicClass() {
        return MongoDataSourceInfo. class;
    }

    /**
     * 判断是否需要路径配置
     *
     * @return 返回false，表示MongoDB数据源不需要额外的路径配置
     */
    @Override
    public boolean requirePath() {
        return false;
    }

    /**
     * 判断是否需要脚本配置
     *
     * @return 返回false，表示MongoDB数据源不需要脚本配置
     */
    @Override
    public boolean requiredScript() {
        return false;
    }

    /**
     * 判断是否允许根路径访问
     *
     * @return 返回true，表示MongoDB数据源允许根路径访问
     */
    @Override
    public boolean allowRoot() {
        return true;
    }

    /**
     * 构建数据源映射键值
     * 根据数据源信息生成唯一的映射键，用于标识不同的数据源实例
     *
     * @param info MongoDB数据源信息对象，包含数据源的关键信息
     * @return 返回由数据源key和更新时间组成的唯一标识符
     */
    @Override
    public String buildMappingKey(MongoDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }

    /**
     * 验证Mongo数据源信息实体的有效性
     * @param entity 待验证的Mongo数据源信息实体对象
     */
    @Override
    public void validate(MongoDataSourceInfo entity) {
        // 校验必要字段
        validateKeyUnique(entity.getKey(), entity.getId());
        validateField(entity);

        // 如果实体ID不为空，需要校验key字段不可修改
        if (entity.getId() != null) {
            MongoDataSourceInfo oldKafkaDataSourceInfo =  magicResourceService.file(entity.getId());
            if (null != oldKafkaDataSourceInfo) {
                if (!oldKafkaDataSourceInfo.getKey().equals(entity.getKey())) {
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
                .map(it -> (MongoDataSourceInfo)it)
                .filter(it -> !it.getId().equals(currentId))
                .noneMatch(it -> Objects.equals(it.getKey(), key));

        if (!noneMatchKey) {
            throw new IllegalArgumentException(String.format("数据源唯一标识(key) '%s' 已存在，请使用其他唯一标识", key));
        }

    }

    /**
     * 设置魔法资源服务
     * @param magicResourceService 魔法资源服务实例
     */
    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    /**
     * 将字节数组反序列化为Mongo数据源信息对象
     * @param bytes 包含序列化数据的字节数组
     * @return 反序列化后的MongoDataSourceInfo对象
     */
    @Override
    public MongoDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, MongoDataSourceInfo.class);
    }

    /**
     * 将实体对象序列化为字节数组
     * @param entity 需要序列化的魔法实体对象
     * @return 序列化后的字节数组
     */
    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    /**
     * 从资源中读取并反序列化Mongo数据源信息
     * @param resource 包含序列化数据的资源对象
     * @return 反序列化后的MongoDataSourceInfo对象
     */
    @Override
    public MongoDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }

}
