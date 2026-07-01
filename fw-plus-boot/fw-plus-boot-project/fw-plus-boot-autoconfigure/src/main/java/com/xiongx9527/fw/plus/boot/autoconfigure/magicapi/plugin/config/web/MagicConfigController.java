package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.config.web;

import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.web.MagicController;
import org.ssssssss.magicapi.core.web.MagicExceptionHandler;

public class MagicConfigController extends MagicController implements MagicExceptionHandler {

    /**
     * 构造函数，用于创建MagicConfigController实例
     *
     * @param configuration 魔法配置对象，用于初始化控制器的配置信息
     */
    public MagicConfigController(MagicConfiguration configuration) {
        super(configuration);
    }

}
