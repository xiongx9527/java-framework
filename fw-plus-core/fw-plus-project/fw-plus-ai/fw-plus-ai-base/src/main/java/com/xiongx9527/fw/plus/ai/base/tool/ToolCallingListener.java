package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * 工具调用环事件监听：步开始 / 步结束 / 单工具开始 / 单工具结束。
 */
public interface ToolCallingListener {

    default void onStepStart() {
    }

    default void onStepEnd() {
    }

    default void onToolStart(String name) {
    }

    default void onToolEnd(String name, boolean ok, long latencyMillis) {
    }
}
