package com.xiongx9527.fw.plus.ai.service;

import com.xiongx9527.fw.plus.ai.memory.ChatMemory;
import com.xiongx9527.fw.plus.ai.memory.ChatMemoryProvider;
import com.xiongx9527.fw.plus.ai.model.chat.ChatModel;
import com.xiongx9527.fw.plus.ai.model.chat.StreamingChatModel;
import com.xiongx9527.fw.plus.ai.model.moderation.ModerationModel;
import com.xiongx9527.fw.plus.ai.rag.RetrievalAugmentor;
import com.xiongx9527.fw.plus.ai.tool.ToolExecutor;
import com.xiongx9527.fw.plus.ai.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.tool.ToolSpecification;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/** AiServices 装配上下文，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.service.AiServiceContext}（字段与官方一致）。 */
public class AiServiceContext {
    public Class<?> aiServiceClass;
    public ChatModel chatModel;
    public StreamingChatModel streamingChatModel;
    public Function<Object, Optional<String>> systemMessageProvider;
    public Map<Object, ChatMemory> chatMemories;
    public ChatMemoryProvider chatMemoryProvider;
    public ModerationModel moderationModel;
    public List<ToolSpecification> toolSpecifications;
    public Map<String, ToolExecutor> toolExecutors;
    public ToolProvider toolProvider;
    public RetrievalAugmentor retrievalAugmentor;
    public AiServiceContext(Class<?> aiServiceClass) { this.aiServiceClass = aiServiceClass; }
}
