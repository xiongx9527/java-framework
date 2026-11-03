package com.xiongx9527.fw.plus.ai.base.model.moderation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 给任意 {@link ModerationModel} 挂监听器的装饰器。
 */
public final class ListeningModerationModel implements ModerationModel {

    private final ModerationModel delegate;

    private final List<ModerationListener> listeners;

    public ListeningModerationModel(ModerationModel delegate, List<ModerationListener> listeners) {
        this.delegate = delegate;
        this.listeners = Collections.unmodifiableList(listeners == null
                ? new ArrayList<ModerationListener>() : new ArrayList<ModerationListener>(listeners));
    }

    @Override
    public ModerationResult moderate(String text) {
        notifyRequest(text);
        ModerationResult result;
        try {
            result = delegate.moderate(text);
        } catch (RuntimeException e) {
            notifyError(e.getMessage());
            throw e;
        }
        notifyResponse(result);
        if (result != null && result.isFlagged()) {
            notifyFlagged(result);
        }
        return result;
    }

    private void notifyRequest(String text) {
        for (ModerationListener listener : listeners) {
            try {
                listener.onRequest(text);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void notifyResponse(ModerationResult result) {
        for (ModerationListener listener : listeners) {
            try {
                listener.onResponse(result);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void notifyFlagged(ModerationResult result) {
        for (ModerationListener listener : listeners) {
            try {
                listener.onFlagged(result);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void notifyError(String message) {
        for (ModerationListener listener : listeners) {
            try {
                listener.onError(message);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }
}
