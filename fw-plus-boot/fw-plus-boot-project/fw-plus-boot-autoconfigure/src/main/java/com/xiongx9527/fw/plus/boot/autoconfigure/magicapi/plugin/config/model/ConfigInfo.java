package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

@Data
public class ConfigInfo extends PathMagicEntity {

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 命名空间
     */
    private String namespace;

    /**
     * 环境
     */
    private String env;

    /**
     * 是否为外部配置
     */
    private boolean externalFlag;

    /**
     * 是否为通用配置
     */
    private boolean commonFlag;

    /**
     * 描述
     */
    private String description;

    /**
     * 提供一个简化的实体信息版本的自己
     * 该方法用于创建当前对象的一个简化版本，主要用于跨服务或模块调用时减少数据传输量
     *
     * @return 返回一个简化版本的MagicEntity对象，这里实际上是返回ConfigInfo对象
     */
    @Override
    public MagicEntity simple() {
        ConfigInfo info = new ConfigInfo();
        super.simple(info);
        info.setAppId(this.appId);
        info.setNamespace(this.namespace);
        info.setEnv(this.env);
        info.setEnabled(this.enabled);
        info.setCommonFlag(this.commonFlag);
        info.setExternalFlag(this.externalFlag);
        info.setDescription(this.description);
        info.setPath(null);
        return info;
    }

    /**
     * 复制当前配置信息对象
     * 该方法用于创建当前对象的一个深拷贝，主要用于在不改变原对象的情况下获取一个与之相同的新对象
     *
     * @return 返回一个新复制的ConfigInfo对象，该对象与当前对象具有相同的属性值
     */
    @Override
    public ConfigInfo copy() {
        ConfigInfo info = new ConfigInfo();
        super.copyTo(info);
        info.setAppId(this.appId);
        info.setNamespace(this.namespace);
        info.setEnv(this.env);
        info.setEnabled(this.enabled);
        info.setCommonFlag(this.commonFlag);
        info.setExternalFlag(this.externalFlag);
        info.setDescription(this.description);
        info.setPath(null);
        return info;
    }
}
