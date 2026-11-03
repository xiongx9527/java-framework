package com.xiongx9527.fw.plus.ai.base.model.moderation;

import com.xiongx9527.fw.plus.ai.base.event.AiListener;

/**
 * 审核事件监听：请求前、响应后、命中时。
 */
public interface ModerationListener extends AiListener {

    default void onRequest(String text) {
    }

    default void onResponse(ModerationResult result) {
    }

    /** 命中违规内容时回调（在 {@link #onResponse} 之后）。 */
    default void onFlagged(ModerationResult result) {
    }
}
