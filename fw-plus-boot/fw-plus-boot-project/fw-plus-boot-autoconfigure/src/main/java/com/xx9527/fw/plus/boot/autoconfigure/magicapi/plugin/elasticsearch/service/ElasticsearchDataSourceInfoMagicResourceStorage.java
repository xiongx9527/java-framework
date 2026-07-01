package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.service;


import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.common.BaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.ElasticSearchDataSourceInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

public class ElasticsearchDataSourceInfoMagicResourceStorage implements MagicResourceStorage<ElasticSearchDataSourceInfo>, BaseService {
    private MagicResourceService magicResourceService;

    @Override
    public String folder() {
        return "elasticsearch-datasource";
    }

    @Override
    public String suffix() {
        return ".json";
    }

    @Override
    public Class<ElasticSearchDataSourceInfo> magicClass() {
        return ElasticSearchDataSourceInfo. class;
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
    public String buildMappingKey(ElasticSearchDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }

    @Override
    public void validate(ElasticSearchDataSourceInfo entity) {
        // 校验必要字段
        validateKeyUnique(entity.getKey(), entity.getId());
        validateField(entity);

        if (entity.getId() != null) {
            ElasticSearchDataSourceInfo oldKafkaDataSourceInfo =  magicResourceService.file(entity.getId());
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
        // 获取所有已存在的数据源
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .map(it -> (ElasticSearchDataSourceInfo)it)
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
    public ElasticSearchDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, ElasticSearchDataSourceInfo.class);
    }

    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    @Override
    public ElasticSearchDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }
}
