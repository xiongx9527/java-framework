package com.xiongx9527.fw.plus.ai.interactive.confirm;

import java.util.concurrent.ConcurrentHashMap;

/** 基于内存的 {@link ConfirmTokenStore} 默认实现（单实例可用；多实例需换成共享存储）。 */
public class InMemoryConfirmTokenStore implements ConfirmTokenStore {

    private final ConcurrentHashMap<String, Boolean> used = new ConcurrentHashMap<String, Boolean>();

    @Override
    public void markUsed(String token) {
        if (token != null) {
            used.put(token, Boolean.TRUE);
        }
    }

    @Override
    public boolean isUsed(String token) {
        return token != null && used.containsKey(token);
    }
}
