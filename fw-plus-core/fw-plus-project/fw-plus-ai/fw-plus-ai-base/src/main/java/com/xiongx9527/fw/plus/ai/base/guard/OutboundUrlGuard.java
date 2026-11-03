package com.xiongx9527.fw.plus.ai.base.guard;

/**
 * 出站 URL 守卫：发起外部请求前校验目标地址。
 */
public interface OutboundUrlGuard {

    /**
     * 校验 URL；不允许时抛 {@link OutboundUrlBlockedException}。
     */
    void check(String url);
}
