package com.xiongx9527.fw.plus.dify.app.types;

/**
 * dify chat-message 消息事件
 * 来源：https://docs.dify.ai/
 */
public class DifyChatEvent {
    public static final String message = "message";
    public static final String nodeRetry = "node_retry";
    public static final String messageFile = "message_file";
    public static final String messageEnd = "message_end";
    public static final String ttsMessage = "tts_message";
    public static final String ttsMessageEnd = "tts_message_end";
    public static final String messageReplace = "message_replace";
    public static final String workflowStarted = "workflow_started";
    public static final String nodeStarted = "node_started";
    public static final String nodeFinished = "node_finished";
    public static final String workflowFinished = "workflow_finished";
    public static final String error = "error";
    public static final String agentMessage = "agent_message";
    public static final String agentThought = "agent_thought";
    public static final String textChunk = "text_chunk";
    public static final String reasoningChunk = "reasoning_chunk";
    public static final String humanInputRequired = "human_input_required";
    public static final String workflowPaused = "workflow_paused";
    public static final String ping = "ping";
}
