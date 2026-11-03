package com.xiongx9527.fw.plus.ai.base.event;

/**
 * 所有 AI 能力监听器的公共父接口。
 *
 * <p>只收敛「失败」这一所有能力共有的事件；请求 / 响应 / 用量等事件由各能力自己的
 * 监听器接口定义，避免用一个万能事件对象把所有字段拍平。</p>
 */
public interface AiListener {

    /** 调用失败；{@code message} 为可直接展示的失败原因。 */
    default void onError(String message) {
    }
}
