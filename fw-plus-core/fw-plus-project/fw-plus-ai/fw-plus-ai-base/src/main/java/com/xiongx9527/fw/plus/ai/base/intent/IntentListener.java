package com.xiongx9527.fw.plus.ai.base.intent;

import com.xiongx9527.fw.plus.ai.base.event.AiListener;

/**
 * 意图分类事件监听。
 */
public interface IntentListener extends AiListener {

    default void onClassify(IntentRequest request) {
    }

    default void onMatched(IntentResult result) {
    }
}
