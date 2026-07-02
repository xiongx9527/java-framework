package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.service;


import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.debezium.model.DebeziumDataSourceInfo;
import lombok.extern.slf4j.Slf4j;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;
@Slf4j
public class DebeziumDataSourceInfoMagicResourceStorage implements MagicResourceStorage<DebeziumDataSourceInfo>, BaseService {

    /**
     * 资源服务实例
     * 用于处理魔法资源相关的业务逻辑
     */
    private MagicResourceService magicResourceService;

    /**
     * 获取文件夹名称
     *
     * @return 返回文件夹名称"debezium-datasource"
     */
    @Override
    public String folder() {
        return "debezium-datasource";
    }

    /**
     * 获取文件后缀名
     *
     * @return 返回文件后缀名".json"
     */
    @Override
    public String suffix() {
        return ".json";
    }

    /**
     * 获取魔法类的类型
     *
     * @return 返回DebeziumDataSourceInfo类的Class对象
     */
    @Override
    public Class<DebeziumDataSourceInfo> magicClass() {
        return DebeziumDataSourceInfo. class;
    }

    /**
     * 判断是否需要路径
     *
     * @return 返回false，表示不需要路径
     */
    @Override
    public boolean requirePath() {
        return false;
    }

    /**
     * 判断是否需要脚本
     *
     * @return 返回false，表示不需要脚本
     */
    @Override
    public boolean requiredScript() {
        return false;
    }

    /**
     * 判断是否允许根路径
     *
     * @return 返回true，表示允许根路径
     */
    @Override
    public boolean allowRoot() {
        return true;
    }

    /**
     * 构建映射键
     *
     * @param info Debezium数据源信息对象
     * @return 返回由key和updateTime组成的映射键，格式为"{key}-{updateTime}"
     */
    @Override
    public String buildMappingKey(DebeziumDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }


    /**
     * 验证Debezium数据源信息实体的有效性
     *
     * @param entity 待验证的Debezium数据源信息实体，不能为空
     */
    @Override
    public void validate(DebeziumDataSourceInfo entity) {
        // 校验必要字段
        validateKeyUnique(entity.getKey(), entity.getId());
        validateField(entity);

        // 检查更新操作中key字段是否被修改
        if (entity.getId() != null && magicResourceService != null) {
            try {
                Object obj = magicResourceService.file(entity.getId());
                if (obj instanceof DebeziumDataSourceInfo) {
                    DebeziumDataSourceInfo oldDebeziumDataSourceInfo = (DebeziumDataSourceInfo) obj;
                    if (null != oldDebeziumDataSourceInfo) {
                        if (!oldDebeziumDataSourceInfo.getKey().equals(entity.getKey())) {
                            throw new IllegalArgumentException("key不可修改");
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取旧数据源信息时发生异常，ID: {}", entity.getId(), e);
                throw e;
            }
        }
    }


    /**
     * 校验数据源key的唯一性
     * 检查给定的key是否在已存在的数据源中唯一，如果已存在相同的key则抛出异常
     *
     * @param key 数据源key
     * @param currentId 当前数据源ID，新增时为null
     */
    private void validateKeyUnique(String key, String currentId) {
        if (key == null || magicResourceService == null) {
            return;
        }
        
        // 获取所有已存在的数据源
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .filter(it -> it instanceof DebeziumDataSourceInfo)
                .map(it -> (DebeziumDataSourceInfo)it)
                .filter(it -> it.getId() != null && !it.getId().equals(currentId))
                .noneMatch(it -> Objects.equals(it.getKey(), key));

        if (!noneMatchKey) {
            throw new IllegalArgumentException(String.format("数据源唯一标识(key) '%s' 已存在，请使用其他唯一标识", key));
        }

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
     * 从字节数组中读取并反序列化为Debezium数据源信息对象
     * @param bytes 包含序列化数据的字节数组
     * @return 反序列化后的Debezium数据源信息对象
     */
    @Override
    public DebeziumDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, DebeziumDataSourceInfo.class);
    }

    /**
     * 将实体对象序列化为JSON格式的字节数组
     * @param entity 需要序列化的魔术实体对象
     * @return 序列化后的字节数组
     */
    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    /**
     * 从资源中读取数据并反序列化为Debezium数据源信息对象
     * @param resource 数据源资源对象
     * @return 反序列化后的Debezium数据源信息对象
     */
    @Override
    public DebeziumDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }



}
