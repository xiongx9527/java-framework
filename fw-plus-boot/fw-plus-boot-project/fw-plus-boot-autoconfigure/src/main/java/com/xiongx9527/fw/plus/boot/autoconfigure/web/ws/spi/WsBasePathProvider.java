package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi;

/**
 * WebSocket 端点基础路径提供者。
 *
 * <p>端点最终路径 = {@link #getBasePath()} + {@code fw.plus.ws.path}。
 * 地基按容器内全部候选提供者与每条相对路径叉乘展开；宿主框架可以提供自己的实现
 * 以复用自身的上下文路径。</p>
 *
 * @author zzy
 */
@FunctionalInterface
public interface WsBasePathProvider {

    /**
     * 获取端点基础路径。
     *
     * @return 基础路径，允许为空串
     */
    String getBasePath();
}
