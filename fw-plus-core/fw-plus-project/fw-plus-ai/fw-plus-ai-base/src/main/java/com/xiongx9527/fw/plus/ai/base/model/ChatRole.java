package com.xiongx9527.fw.plus.ai.base.model;

/**
 * 对话角色。
 */
public enum ChatRole {

    SYSTEM("system"),

    USER("user"),

    ASSISTANT("assistant"),

    /** 工具执行结果回填（OpenAI 的 {@code role: "tool"}）。 */
    TOOL("tool");

    private final String wireName;

    ChatRole(String wireName) {
        this.wireName = wireName;
    }

    /** 上游报文中使用的角色名。 */
    public String wireName() {
        return wireName;
    }

    /**
     * 按上游角色名反解；无法识别时返回 {@code null}。
     */
    public static ChatRole fromWireName(String name) {
        if (name == null) {
            return null;
        }
        String normalized = name.trim();
        for (ChatRole role : values()) {
            if (role.wireName.equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        return null;
    }
}
