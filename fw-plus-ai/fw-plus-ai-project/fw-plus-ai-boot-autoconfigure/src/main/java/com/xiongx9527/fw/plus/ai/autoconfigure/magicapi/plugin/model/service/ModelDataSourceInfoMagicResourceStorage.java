package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.ModelDataSourceInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

public class ModelDataSourceInfoMagicResourceStorage implements MagicResourceStorage<ModelDataSourceInfo> {

    private MagicResourceService magicResourceService;

    @Override
    public String folder() { return "model-datasource"; }

    @Override
    public String suffix() { return ".json"; }

    @Override
    public Class<ModelDataSourceInfo> magicClass() { return ModelDataSourceInfo.class; }

    @Override
    public boolean requirePath() { return false; }

    @Override
    public boolean requiredScript() { return false; }

    @Override
    public boolean allowRoot() { return true; }

    @Override
    public String buildMappingKey(ModelDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }

    @Override
    public void validate(ModelDataSourceInfo entity) {
        validateRequired(entity.getKey(), "key");
        validateRequired(entity.getProvider(), "provider");
        validateKeyUnique(entity.getKey(), entity.getId());
        if (entity.getId() != null && magicResourceService != null) {
            ModelDataSourceInfo old = magicResourceService.file(entity.getId());
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
    public ModelDataSourceInfo read(byte[] bytes) { return JsonUtils.readValue(bytes, ModelDataSourceInfo.class); }

    @Override
    public byte[] write(MagicEntity entity) { return JsonUtils.toJsonBytes(entity); }

    @Override
    public ModelDataSourceInfo readResource(Resource resource) { return read(resource.read()); }

    private void validateKeyUnique(String key, String currentId) {
        if (key == null || magicResourceService == null) {
            return;
        }
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .map(it -> (ModelDataSourceInfo) it)
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
