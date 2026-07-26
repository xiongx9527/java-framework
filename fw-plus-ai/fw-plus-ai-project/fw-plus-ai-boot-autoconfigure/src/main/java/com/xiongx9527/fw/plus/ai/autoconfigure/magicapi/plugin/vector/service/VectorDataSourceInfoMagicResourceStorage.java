package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.VectorDataSourceInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

public class VectorDataSourceInfoMagicResourceStorage implements MagicResourceStorage<VectorDataSourceInfo> {

    private MagicResourceService magicResourceService;

    @Override
    public String folder() {
        return "vector-datasource";
    }

    @Override
    public String suffix() {
        return ".json";
    }

    @Override
    public Class<VectorDataSourceInfo> magicClass() {
        return VectorDataSourceInfo.class;
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
    public String buildMappingKey(VectorDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }

    @Override
    public void validate(VectorDataSourceInfo entity) {
        validateRequired(entity.getKey(), "key");
        validateRequired(entity.getProvider(), "provider");
        validateKeyUnique(entity.getKey(), entity.getId());
        if (entity.getId() != null && magicResourceService != null) {
            VectorDataSourceInfo old = magicResourceService.file(entity.getId());
            if (old != null && !Objects.equals(old.getKey(), entity.getKey())) {
                throw new IllegalArgumentException("key不可修改");
            }
        }
    }

    @Override
    public void setMagicResourceService(MagicResourceService magicResourceService) {
        this.magicResourceService = magicResourceService;
    }

    @Override
    public VectorDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, VectorDataSourceInfo.class);
    }

    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    @Override
    public VectorDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }

    private void validateKeyUnique(String key, String currentId) {
        if (key == null || magicResourceService == null) {
            return;
        }
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .map(it -> (VectorDataSourceInfo) it)
                .filter(it -> !Objects.equals(it.getId(), currentId))
                .noneMatch(it -> Objects.equals(it.getKey(), key));
        if (!noneMatchKey) {
            throw new IllegalArgumentException(String.format("数据源唯一标识(key) '%s' 已存在，请使用其他唯一标识", key));
        }
    }

    private void validateRequired(String value, String field) {
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(field + "不能为空");
        }
    }
}
