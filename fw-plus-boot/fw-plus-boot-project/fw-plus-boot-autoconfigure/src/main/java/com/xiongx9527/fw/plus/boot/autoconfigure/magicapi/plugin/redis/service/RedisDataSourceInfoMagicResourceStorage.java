package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.service;


import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

public class RedisDataSourceInfoMagicResourceStorage implements MagicResourceStorage<RedisDataSourceInfo>, BaseService {
    private MagicResourceService magicResourceService;

    @Override
    public String folder() {
        return "redis-datasource";
    }

    @Override
    public String suffix() {
        return ".json";
    }

    @Override
    public Class<RedisDataSourceInfo> magicClass() {
        return RedisDataSourceInfo.class;
    }

    @Override
    public boolean requirePath() {
        return false;
    }

    @Override
    public boolean requiredScript() {
        return false;
    }

    @Override
    public boolean allowRoot() {
        return true;
    }

    @Override
    public String buildMappingKey(RedisDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime() != null ? info.getUpdateTime() : "");
    }

    @Override
    public void validate(RedisDataSourceInfo entity) {
        // 校验必要字段
        validateKeyUnique(entity.getKey(), entity.getId());
        validateField(entity);

        if (entity.getId() != null) {
            RedisDataSourceInfo oldRedisDataSourceInfo = magicResourceService.file(entity.getId());
            if (null != oldRedisDataSourceInfo) {
                if (!oldRedisDataSourceInfo.getKey().equals(entity.getKey())) {
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
                .map(it -> (RedisDataSourceInfo)it)
                .filter(it -> !it.getId().equals(currentId))
                .noneMatch(it -> Objects.equals(it.getKey(), key));

        if (!noneMatchKey) {
            throw new IllegalArgumentException(String.format("数据源唯一标识(key) '%s' 已存在，请使用其他唯一标识", key));
        }

    }

    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    @Override
    public RedisDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, RedisDataSourceInfo.class);
    }

    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    @Override
    public RedisDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }
}
