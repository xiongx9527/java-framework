package com.xiongx9527.fw.plus.ai.base.guard;

/**
 * 护栏检查结果。
 *
 * <p>{@link Action#PASS} 放行；{@code REWRITE} 用 {@link #getContent()} 改写文本；
 * {@code RETRY} 让调用方带着提示重试；{@code REJECT} 直接拒绝。</p>
 */
public final class GuardrailResult {

    public enum Action {
        PASS,
        REWRITE,
        RETRY,
        REJECT
    }

    private static final GuardrailResult PASS = new GuardrailResult(Action.PASS, null, null);

    private final Action action;

    private final String content;

    private final String reason;

    private GuardrailResult(Action action, String content, String reason) {
        this.action = action;
        this.content = content;
        this.reason = reason;
    }

    public static GuardrailResult pass() {
        return PASS;
    }

    public static GuardrailResult rewrite(String content) {
        return new GuardrailResult(Action.REWRITE, content, null);
    }

    public static GuardrailResult retry(String hint) {
        return new GuardrailResult(Action.RETRY, hint, hint);
    }

    public static GuardrailResult reject(String reason) {
        return new GuardrailResult(Action.REJECT, null, reason);
    }

    public Action getAction() {
        return action;
    }

    public String getContent() {
        return content;
    }

    public String getReason() {
        return reason;
    }

    public boolean isPass() {
        return action == Action.PASS;
    }

    /** 是否应终止本次调用（REJECT）。 */
    public boolean isTerminal() {
        return action == Action.REJECT;
    }
}
