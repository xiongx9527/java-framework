package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.satoken.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.PathMagicEntity;
@Data
public class SaTokenInfo  extends PathMagicEntity {

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 描述
     */
    private String description;

    /**
     * 参数
     */
    private String params;
}
