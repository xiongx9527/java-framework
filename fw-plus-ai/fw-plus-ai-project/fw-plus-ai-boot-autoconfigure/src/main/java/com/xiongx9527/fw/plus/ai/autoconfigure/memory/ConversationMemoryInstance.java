package com.xiongx9527.fw.plus.ai.autoconfigure.memory;

import com.xiongx9527.fw.plus.ai.autoconfigure.model.AbstractModelInstance;

/**
 * 单个对话记忆后端实例配置。继承 {@link AbstractModelInstance} 的公共连接 / 鉴权 / 超时字段，
 * 并补充记忆特有的 {@code authScheme}（{@code API_KEY} / {@code BEARER}）。
 */
public class ConversationMemoryInstance extends AbstractModelInstance {

    /** mem0 服务端鉴权方案。 */
    private String authScheme = "API_KEY";

    public String getAuthScheme() {
        return authScheme;
    }

    public void setAuthScheme(String authScheme) {
        this.authScheme = authScheme;
    }
}
