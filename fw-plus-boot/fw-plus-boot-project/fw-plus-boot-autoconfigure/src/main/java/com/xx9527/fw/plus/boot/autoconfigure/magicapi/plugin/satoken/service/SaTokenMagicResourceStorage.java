package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.satoken.service;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.satoken.model.SaTokenInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.model.SseDataSourceInfo;
import org.ssssssss.magicapi.core.config.JsonCodeConstants;
import org.ssssssss.magicapi.core.model.JsonCode;
import org.ssssssss.magicapi.core.service.AbstractPathMagicResourceStorage;

import java.util.List;
import java.util.Objects;

public class SaTokenMagicResourceStorage extends AbstractPathMagicResourceStorage<SaTokenInfo> implements JsonCodeConstants {
    @Override
    public String folder() {
        return "satoken";
    }

    @Override
    public boolean requirePath() {
        return false;
    }

    @Override
    public Class<SaTokenInfo> magicClass() {
        return SaTokenInfo.class;
    }

    @Override
    public String buildMappingKey(SaTokenInfo entity) {
        return buildMappingKey(entity, magicResourceService.getGroupPath(entity.getGroupId()));
    }

    @Override
    public void validate(SaTokenInfo entity) {
        notBlank(entity.getBizType(), new JsonCode(990, "业务类型不能为空"));
        boolean noneMatchKey = magicResourceService.listFiles(folder() + ":0").stream()
                .map(it -> (SaTokenInfo)it)
                .filter(it -> !it.getId().equals(entity.getId()))
                .noneMatch(it -> Objects.equals(it.getBizType(), entity.getBizType()));
        isTrue(noneMatchKey, new JsonCode(991, "业务类型已存在"));
    }

    public List<SaTokenInfo> findAll() {
        return magicResourceService.files(folder());
    }
}
