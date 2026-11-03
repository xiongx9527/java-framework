package com.xiongx9527.fw.plus.ai.base.service;

import com.xiongx9527.fw.plus.ai.base.intent.IntentPort;
import com.xiongx9527.fw.plus.ai.base.intent.IntentScoped;
import com.xiongx9527.fw.plus.ai.base.memory.ChatMemoryProvider;
import com.xiongx9527.fw.plus.ai.base.model.LlmListener;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationListener;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationModel;
import com.xiongx9527.fw.plus.ai.base.output.JsonOutputPort;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalAugmentor;
import com.xiongx9527.fw.plus.ai.base.skill.SkillBox;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 服务的运行上下文：模型端口 + 记忆 + 意图分类 + 按意图作用域分组的工具/检索/Skill + 监听器。
 *
 * <p>由 {@link AiServices} 的 builder 组装，代理在每次调用时读取。工具、检索增强、Skill 目录
 * 都通过 {@link IntentScoped} 绑定到具体意图（或全局），调用时按意图选择。</p>
 */
public final class AiServiceContext {

    private final Class<?> serviceType;

    private final LlmPort llm;

    private final ChatMemoryProvider chatMemoryProvider;

    private final IntentPort intentClassifier;

    private final List<IntentScoped<ToolProvider>> toolProviderScopes;

    private final List<IntentScoped<RetrievalAugmentor>> retrievalScopes;

    private final List<IntentScoped<SkillBox>> skillScopes;

    private final List<LlmListener> listeners;

    private final boolean emitLifecycleEvents;

    private final boolean skipToolsWhenRetrievalHit;

    private final ModerationModel moderationModel;

    private final List<ModerationListener> moderationListeners;

    private final JsonOutputPort jsonOutput;

    private final List<AiObservationListener> observationListeners;

    AiServiceContext(Class<?> serviceType, LlmPort llm, ChatMemoryProvider chatMemoryProvider,
                     IntentPort intentClassifier,
                     List<IntentScoped<ToolProvider>> toolProviderScopes,
                     List<IntentScoped<RetrievalAugmentor>> retrievalScopes,
                     List<IntentScoped<SkillBox>> skillScopes,
                     List<LlmListener> listeners,
                     boolean emitLifecycleEvents, boolean skipToolsWhenRetrievalHit,
                     ModerationModel moderationModel, List<ModerationListener> moderationListeners,
                     JsonOutputPort jsonOutput, List<AiObservationListener> observationListeners) {
        this.serviceType = serviceType;
        this.llm = llm;
        this.chatMemoryProvider = chatMemoryProvider;
        this.intentClassifier = intentClassifier;
        this.toolProviderScopes = toolProviderScopes == null ? Collections.<IntentScoped<ToolProvider>>emptyList()
                : Collections.unmodifiableList(new ArrayList<IntentScoped<ToolProvider>>(toolProviderScopes));
        this.retrievalScopes = retrievalScopes == null ? Collections.<IntentScoped<RetrievalAugmentor>>emptyList()
                : Collections.unmodifiableList(new ArrayList<IntentScoped<RetrievalAugmentor>>(retrievalScopes));
        this.skillScopes = skillScopes == null ? Collections.<IntentScoped<SkillBox>>emptyList()
                : Collections.unmodifiableList(new ArrayList<IntentScoped<SkillBox>>(skillScopes));
        this.listeners = listeners == null ? Collections.<LlmListener>emptyList()
                : Collections.unmodifiableList(new ArrayList<LlmListener>(listeners));
        this.emitLifecycleEvents = emitLifecycleEvents;
        this.skipToolsWhenRetrievalHit = skipToolsWhenRetrievalHit;
        this.moderationModel = moderationModel;
        this.moderationListeners = moderationListeners == null ? Collections.<ModerationListener>emptyList()
                : Collections.unmodifiableList(new ArrayList<ModerationListener>(moderationListeners));
        this.jsonOutput = jsonOutput;
        this.observationListeners = observationListeners == null ? Collections.<AiObservationListener>emptyList()
                : Collections.unmodifiableList(new ArrayList<AiObservationListener>(observationListeners));
    }

    public Class<?> getServiceType() {
        return serviceType;
    }

    public LlmPort getLlm() {
        return llm;
    }

    public ChatMemoryProvider getChatMemoryProvider() {
        return chatMemoryProvider;
    }

    public IntentPort getIntentClassifier() {
        return intentClassifier;
    }

    public List<IntentScoped<ToolProvider>> getToolProviderScopes() {
        return toolProviderScopes;
    }

    public List<IntentScoped<RetrievalAugmentor>> getRetrievalScopes() {
        return retrievalScopes;
    }

    public List<IntentScoped<SkillBox>> getSkillScopes() {
        return skillScopes;
    }

    public List<LlmListener> getListeners() {
        return listeners;
    }

    /** 是否向 {@code LlmStreamHandler} 派发意图 / 检索 / 注入 / 工具 等生命周期事件。 */
    public boolean isEmitLifecycleEvents() {
        return emitLifecycleEvents;
    }

    /** 检索已命中上下文时是否跳过工具调用（对齐 langchain4j 的“检索优先、命中即答”行为）。 */
    public boolean isSkipToolsWhenRetrievalHit() {
        return skipToolsWhenRetrievalHit;
    }

    /** 内容审核模型；为 {@code null} 表示不开启审核。 */
    public ModerationModel getModerationModel() {
        return moderationModel;
    }

    /** 审核事件监听。 */
    public List<ModerationListener> getModerationListeners() {
        return moderationListeners;
    }

    /** 稳定 JSON 输出端口；为 {@code null} 表示不在 AI 服务层强制 JSON。 */
    public JsonOutputPort getJsonOutput() {
        return jsonOutput;
    }

    /** 调用级观测监听（用于 Langfuse / Langsmith 等）。 */
    public List<AiObservationListener> getObservationListeners() {
        return observationListeners;
    }
}
