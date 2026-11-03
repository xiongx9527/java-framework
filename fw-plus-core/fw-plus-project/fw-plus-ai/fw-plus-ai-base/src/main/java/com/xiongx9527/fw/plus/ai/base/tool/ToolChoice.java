package com.xiongx9527.fw.plus.ai.base.tool;

/**
 * 工具选择策略：告诉模型「可以调、别调、必须调、只调这一个」。
 *
 * <p>对应上游的 {@code tool_choice} 参数，与具体厂商无关；adapter 负责翻译成
 * OpenAI 的 {@code "auto" / "none" / "required" / {"type":"function","function":{"name":...}}}
 * 或其它协议。</p>
 */
public final class ToolChoice {

    /** 选择模式。 */
    public enum Mode {

        /** 模型自行决定是否调用。 */
        AUTO,

        /** 禁止调用工具。 */
        NONE,

        /** 必须调用至少一个工具。 */
        REQUIRED,

        /** 只允许调用指定名字的工具。 */
        NAMED
    }

    private static final ToolChoice AUTO = new ToolChoice(Mode.AUTO, null);

    private static final ToolChoice NONE = new ToolChoice(Mode.NONE, null);

    private static final ToolChoice REQUIRED = new ToolChoice(Mode.REQUIRED, null);

    private final Mode mode;

    private final String name;

    private ToolChoice(Mode mode, String name) {
        this.mode = mode;
        this.name = name;
    }

    public static ToolChoice auto() {
        return AUTO;
    }

    public static ToolChoice none() {
        return NONE;
    }

    public static ToolChoice required() {
        return REQUIRED;
    }

    public static ToolChoice named(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("tool name is required");
        }
        return new ToolChoice(Mode.NAMED, name);
    }

    public Mode getMode() {
        return mode;
    }

    /** 仅 {@link Mode#NAMED} 时有值。 */
    public String getName() {
        return name;
    }
}
