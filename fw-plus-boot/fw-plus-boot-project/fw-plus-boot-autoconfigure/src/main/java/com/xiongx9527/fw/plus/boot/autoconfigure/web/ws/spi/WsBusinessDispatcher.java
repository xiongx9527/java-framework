package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi;

import java.util.List;

/**
 * 业务侧订阅调度 SPI。
 *
 * <p>WebSocket 地基只负责连接、鉴权、会话、心跳与广播；「哪些订阅生效」「脚本怎么执行」
 * 由实现方决定。未提供实现时，地基退化为纯 WebSocket 通道（不做 appId 校验、不派发业务消息）。</p>
 *
 * @author zzy
 */
public interface WsBusinessDispatcher {

    /**
     * 判断 appId 是否已被业务侧登记。
     *
     * <p>返回 {@code false} 时地基会以「AppId 不存在」拒绝该连接。默认放行。</p>
     *
     * @param appId 应用 ID
     * @return true 表示允许建立连接
     */
    default boolean existsApp(String appId) {
        return true;
    }

    /**
     * 解析指定 appId 与业务类型下启用中的订阅。
     *
     * @param appId        应用 ID
     * @param businessType 业务类型
     * @return 订阅列表，无匹配时返回空列表
     */
    List<? extends WsSubscribeScript> findEnabledSubscribes(String appId, String businessType);

    /**
     * 执行订阅脚本。
     *
     * @param subscribe 订阅描述
     * @param context   调用上下文
     * @return 脚本执行结果
     */
    Object execute(WsSubscribeScript subscribe, WsInvocationContext context);
}
