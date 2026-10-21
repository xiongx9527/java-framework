package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.service;


import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.model.DtpDataSourceInfo;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.utils.IoUtils;
import org.ssssssss.magicapi.utils.JsonUtils;

import java.util.Objects;

public class DtpDataSourceInfoMagicResourceStorage implements MagicResourceStorage<DtpDataSourceInfo>, BaseService {

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
        return "dtp-datasource";
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

    @Override
    public Class<DtpDataSourceInfo> magicClass() {
        return DtpDataSourceInfo.class;
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

    @Override
    public String buildMappingKey(DtpDataSourceInfo info) {
        return String.format("%s-%s", info.getKey(), info.getUpdateTime());
    }

    @Override
    public void validate(DtpDataSourceInfo entity) {
        validateField(entity);
        if (!IoUtils.validateFileName(entity.getKey())) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.dtp.keyInvalidFileName"));
        }
        validateKeyUnique(entity.getKey(), entity.getId());

        if (entity.getId() != null) {
            DtpDataSourceInfo oldInfo = magicResourceService.file(entity.getId());
            if (null != oldInfo && !oldInfo.getKey().equals(entity.getKey())) {
                throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.dtp.keyImmutable"));
            }
        }
    }

    private void validateKeyUnique(String key, String currentId) {
        if (key == null || magicResourceService == null) {
            return;
        }
        boolean exists = magicResourceService.listFiles(folder() + ":0").stream()
                .map(DtpDataSourceInfo.class::cast)
                .anyMatch(it -> !it.getId().equals(currentId) && Objects.equals(it.getKey(), key));
        if (exists) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.dtp.datasourceKeyExists", key));
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

    @Override
    public DtpDataSourceInfo read(byte[] bytes) {
        return JsonUtils.readValue(bytes, DtpDataSourceInfo.class);
    }

    @Override
    public byte[] write(MagicEntity entity) {
        return JsonUtils.toJsonBytes(entity);
    }

    @Override
    public DtpDataSourceInfo readResource(Resource resource) {
        return read(resource.read());
    }

}
