package com.xiongx9527.fw.plus.ai.base.model;

import com.xiongx9527.fw.plus.ai.base.event.Phase;

/**
 * 上下文注入事件：某段外部上下文被注入到本轮对话，带 {@link Phase} 起止。
 *
 * <ul>
 *   <li>{@code START}：即将注入（{@link #getContent()} 为预备注入的内容）。</li>
 *   <li>{@code END}：已注入完成。</li>
 * </ul>
 * 来源由 {@link #getKind()} 标明（检索上下文 / 技能目录 / 记忆 / 系统补丁）。
 */
public final class ContextInjectEvent {

    /** 注入来源类型。 */
    public enum Kind {
        /** 检索增强得到的上下文。 */
        RETRIEVAL,
        /** 技能目录 / 工具说明。 */
        SKILL,
        /** 会话记忆。 */
        MEMORY,
        /** 其他系统级补丁。 */
        SYSTEM
    }

    private final Phase phase;

    private final Kind kind;

    private final String content;

    private final String source;

    private ContextInjectEvent(Phase phase, Kind kind, String content, String source) {
        this.phase = phase;
        this.kind = kind;
        this.content = content;
        this.source = source;
    }

    public static ContextInjectEvent start(Kind kind, String content, String source) {
        return new ContextInjectEvent(Phase.START, kind, content, source);
    }

    public static ContextInjectEvent end(Kind kind, String content, String source) {
        return new ContextInjectEvent(Phase.END, kind, content, source);
    }

    public Phase getPhase() {
        return phase;
    }

    public Kind getKind() {
        return kind;
    }

    public String getContent() {
        return content;
    }

    /** 可选的来源标识（如检索集合名 / 技能 id）。 */
    public String getSource() {
        return source;
    }
}
