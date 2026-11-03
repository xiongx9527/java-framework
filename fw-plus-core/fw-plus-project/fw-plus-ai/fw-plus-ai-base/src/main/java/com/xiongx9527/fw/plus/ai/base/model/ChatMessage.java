package com.xiongx9527.fw.plus.ai.base.model;

import com.xiongx9527.fw.plus.ai.base.model.content.ContentPart;
import com.xiongx9527.fw.plus.ai.base.model.content.ContentType;
import com.xiongx9527.fw.plus.ai.base.model.content.TextContent;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 一条对话消息：角色 + 若干内容片段（多模态）。
 *
 * <p>纯文本消息是「只有一个文本片段」的退化形态，{@link #getContent()} 仍返回拼好的文本，
 * 老调用方无需改动；需要图片 / 音频 / 文件时用 {@link #of(ChatRole, List)} 或
 * {@link #user(List)} 传入片段列表，adapter 按上游协议转换。</p>
 *
 * <p>工具调用相关：助手消息可携带 {@link #getToolCalls() 工具调用列表}，
 * 工具结果消息用 {@link #tool(String, String, String)} 构造，通过
 * {@link #getToolCallId()} 与调用配对。</p>
 */
public final class ChatMessage {

    private final ChatRole role;

    private final List<ContentPart> parts;

    private final List<ToolExecutionRequest> toolCalls;

    private final String toolCallId;

    private final String name;

    private ChatMessage(ChatRole role, List<ContentPart> parts, List<ToolExecutionRequest> toolCalls,
                        String toolCallId, String name) {
        this.role = role;
        this.parts = Collections.unmodifiableList(new ArrayList<ContentPart>(parts));
        this.toolCalls = Collections.unmodifiableList(new ArrayList<ToolExecutionRequest>(toolCalls));
        this.toolCallId = toolCallId;
        this.name = name;
    }

    public static ChatMessage of(ChatRole role, String text) {
        return new ChatMessage(role, Collections.<ContentPart>singletonList(TextContent.of(text)),
                new ArrayList<ToolExecutionRequest>(), null, null);
    }

    public static ChatMessage of(ChatRole role, List<ContentPart> parts) {
        return new ChatMessage(role, parts == null ? new ArrayList<ContentPart>() : parts,
                new ArrayList<ToolExecutionRequest>(), null, null);
    }

    public static ChatMessage of(ChatRole role, ContentPart... parts) {
        return new ChatMessage(role, parts == null ? new ArrayList<ContentPart>() : Arrays.asList(parts),
                new ArrayList<ToolExecutionRequest>(), null, null);
    }

    public static ChatMessage system(String text) {
        return of(ChatRole.SYSTEM, text);
    }

    public static ChatMessage user(String text) {
        return of(ChatRole.USER, text);
    }

    public static ChatMessage assistant(String text) {
        return of(ChatRole.ASSISTANT, text);
    }

    /** 多模态用户消息（文本 + 图片 / 音频 / 文件任意组合）。 */
    public static ChatMessage user(List<ContentPart> parts) {
        return of(ChatRole.USER, parts);
    }

    /**
     * 携带工具调用的助手消息：回灌给模型时必须带上，否则模型不知道上一轮请求过什么。
     *
     * @param calls   模型给出的工具调用
     * @param content 同时产出的正文，可为 {@code null}
     */
    public static ChatMessage assistantWithToolCalls(List<ToolExecutionRequest> calls, String content) {
        List<ContentPart> parts = content == null
                ? new ArrayList<ContentPart>()
                : Collections.<ContentPart>singletonList(TextContent.of(content));
        return new ChatMessage(ChatRole.ASSISTANT, parts,
                calls == null ? new ArrayList<ToolExecutionRequest>() : calls, null, null);
    }

    /**
     * 工具结果消息。
     *
     * @param toolCallId 对应的调用 id
     * @param name       工具名（部分协议必填，可为 {@code null}）
     * @param content    结果文本（调用方负责截断）
     */
    public static ChatMessage tool(String toolCallId, String name, String content) {
        return new ChatMessage(ChatRole.TOOL, Collections.<ContentPart>singletonList(TextContent.of(content)),
                new ArrayList<ToolExecutionRequest>(), toolCallId, name);
    }

    public ChatRole getRole() {
        return role;
    }

    /** 全部内容片段，顺序即上游报文的顺序。 */
    public List<ContentPart> getParts() {
        return parts;
    }

    /** 文本片段拼接结果（忽略图片 / 音频 / 文件）；无文本片段时返回 {@code null}。 */
    public String getContent() {
        StringBuilder sb = new StringBuilder();
        for (ContentPart part : parts) {
            if (part != null && part.type() == ContentType.TEXT && part.text() != null) {
                sb.append(part.text());
            }
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** 是否含非文本片段（图片 / 音频 / 文件）。 */
    public boolean hasMedia() {
        for (ContentPart part : parts) {
            if (part != null && part.type() != ContentType.TEXT) {
                return true;
            }
        }
        return false;
    }

    /** 助手消息携带的工具调用；其它角色为空列表。 */
    public List<ToolExecutionRequest> getToolCalls() {
        return toolCalls;
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    /** 工具结果消息对应的调用 id；非工具消息为 {@code null}。 */
    public String getToolCallId() {
        return toolCallId;
    }

    /** 工具名；仅工具消息有值。 */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "ChatMessage{role=" + role + ", parts=" + parts + ", toolCalls=" + toolCalls + '}';
    }
}
