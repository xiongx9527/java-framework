package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.handler;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.starter.MagicAPISseConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DynamicSseConfigurer {

    /**
     * 动态SSE配置器实例，用于处理动态SSE连接
     */
    private final BaseDynamicSseHandler dynamicWebSocketHandler;

    /**
     * MagicAPI SSE配置实例，包含SSE相关的配置信息
     */
    private final MagicAPISseConfig config;

    /**
     * 构造函数，初始化动态SSE配置器
     *
     * @param dynamicWebSocketHandler 动态SSE处理器，用于处理SSE连接的动态行为
     * @param config MagicAPI SSE配置对象，包含SSE连接的相关配置参数
     */
    public DynamicSseConfigurer(BaseDynamicSseHandler dynamicWebSocketHandler, MagicAPISseConfig config) {
        this.dynamicWebSocketHandler = dynamicWebSocketHandler;
        this.config = config;
    }

}
