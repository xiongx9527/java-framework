package com.xiongx9527.fw.plus.ai.base.service;

import com.xiongx9527.fw.plus.ai.base.intent.IntentPort;
import com.xiongx9527.fw.plus.ai.base.intent.IntentScoped;
import com.xiongx9527.fw.plus.ai.base.memory.ChatMemoryProvider;
import com.xiongx9527.fw.plus.ai.base.model.LlmListener;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationListener;
import com.xiongx9527.fw.plus.ai.base.model.moderation.ModerationModel;
import com.xiongx9527.fw.plus.ai.base.output.JsonBindings;
import com.xiongx9527.fw.plus.ai.base.output.JsonOutputPort;
import com.xiongx9527.fw.plus.ai.base.store.StoreRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalAugmentor;
import com.xiongx9527.fw.plus.ai.base.skill.SkillBox;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;

/**
 * AI 服务的运行上下文：模型端口 + 记忆 + 意图分类 + 按意图作用域分组的工具/检索/Skill + 监听器。
 *
 * <p>由 {@link AiServices} 的 builder 组装，代理在每次调用时读取。工具、检索增强、Skill、记忆与
 * 意图分类器都通过 {@link IntentScoped} 绑定到具体意图（或全局），调用时按意图选择；分类器因先于
 * 意图产生，顶层调用恒走全局分类器，意图特定分类器用于嵌套 / 智能体二次路由。</p>
 */
public final class AiServiceContext {

    private final Class<?> serviceType;

    private final LlmPort llm;

    private final List<IntentScoped<ChatMemoryProvider>> memoryScopes;

    private final List<IntentScoped<IntentPort>> intentScopes;

    private final List<IntentScoped<ToolProvider>> toolProviderScopes;

    private final List<IntentScoped<RetrievalAugmentor>> retrievalScopes;

    private final List<IntentScoped<SkillBox>> skillScopes;

    private final List<LlmListener> listeners;

    private final boolean emitLifecycleEvents;

    private final boolean skipToolsWhenRetrievalHit;

    private final ModerationModel moderationModel;

    private final List<ModerationListener> moderationListeners;

    private final JsonOutputPort jsonOutput;

    private final JsonBindings jsonBindings;

    private final List<AiObservationListener> observationListeners;

    private final StoreRegistry storeRegistry;

    /** 覆盖注解的 system 文本（替代 {@code @SystemMessage}）。 */
    private final String systemTextOverride;

    /** 覆盖注解的 user 模板（替代 {@code @UserMessage}）。 */
    private final String userTemplateOverride;

    /** 动态会话 id（覆盖 {@code @MemoryId}）：每次调用实时计算。 */
    private final java.util.function.Supplier<String> memoryIdOverride;

    /** 会话 id 绑定的方法参数下标（与 {@code memoryIdOverride} 二选一）。 */
    private final Integer memoryIdParamIndex;

    /** 变量绑定覆盖：变量名 -> 方法参数下标（替代 {@code @V}）。 */
    private final Map<String, Integer> varOverrides;

    /** 动态自定义变量：每次调用实时计算后并入模板（不依赖固定值）。 */
    private final Map<String, java.util.function.Supplier<Object>> customVariables;

    AiServiceContext(Class<?> serviceType, LlmPort llm,
                     List<IntentScoped<ChatMemoryProvider>> memoryScopes,
                     List<IntentScoped<IntentPort>> intentScopes,
                     List<IntentScoped<ToolProvider>> toolProviderScopes,
                     List<IntentScoped<RetrievalAugmentor>> retrievalScopes,
                     List<IntentScoped<SkillBox>> skillScopes,
                     List<LlmListener> listeners,
                     boolean emitLifecycleEvents, boolean skipToolsWhenRetrievalHit,
                     ModerationModel moderationModel, List<ModerationListener> moderationListeners,
                     JsonOutputPort jsonOutput, JsonBindings jsonBindings,
                     List<AiObservationListener> observationListeners,
                     String systemTextOverride, String userTemplateOverride,
                     java.util.function.Supplier<String> memoryIdOverride, Integer memoryIdParamIndex,
                     Map<String, Integer> varOverrides,
                     Map<String, java.util.function.Supplier<Object>> customVariables,
                     StoreRegistry storeRegistry) {
        this.serviceType = serviceType;
        this.llm = llm;
        this.memoryScopes = memoryScopes == null ? Collections.<IntentScoped<ChatMemoryProvider>>emptyList()
                : Collections.unmodifiableList(new ArrayList<IntentScoped<ChatMemoryProvider>>(memoryScopes));
        this.intentScopes = intentScopes == null ? Collections.<IntentScoped<IntentPort>>emptyList()
                : Collections.unmodifiableList(new ArrayList<IntentScoped<IntentPort>>(intentScopes));
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
        this.jsonBindings = jsonBindings;
        this.observationListeners = observationListeners == null ? Collections.<AiObservationListener>emptyList()
                : Collections.unmodifiableList(new ArrayList<AiObservationListener>(observationListeners));
        this.systemTextOverride = systemTextOverride;
        this.userTemplateOverride = userTemplateOverride;
        this.memoryIdOverride = memoryIdOverride;
        this.memoryIdParamIndex = memoryIdParamIndex;
        this.varOverrides = varOverrides == null ? Collections.<String, Integer>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(varOverrides));
        this.customVariables = customVariables == null ? Collections.<String, java.util.function.Supplier<Object>>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, java.util.function.Supplier<Object>>(customVariables));
        this.storeRegistry = storeRegistry == null ? new StoreRegistry() : storeRegistry;
    }

    public Class<?> getServiceType() {
        return serviceType;
    }

    public LlmPort getLlm() {
        return llm;
    }

    public List<IntentScoped<ChatMemoryProvider>> getMemoryScopes() {
        return memoryScopes;
    }

    public List<IntentScoped<IntentPort>> getIntentScopes() {
        return intentScopes;
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

    /** JSON 绑定器：把 {@link JsonOutputPort} 提取的文本反序列化为 POJO；为 {@code null} 时回退为原始 JSON 文本。 */
    public JsonBindings getJsonBindings() {
        return jsonBindings;
    }

    /** 调用级观测监听（用于 Langfuse / Langsmith 等）。 */
    public List<AiObservationListener> getObservationListeners() {
        return observationListeners;
    }

    /** 命名存储注册器（向量 / 图 / 自定义后端的统一入口，按名或类型取用）。 */
    public StoreRegistry getStoreRegistry() {
        return storeRegistry;
    }

    /** 覆盖 {@code @SystemMessage} 的文本；为 {@code null} 时回退到注解。 */
    public String getSystemTextOverride() {
        return systemTextOverride;
    }

    /** 覆盖 {@code @UserMessage} 的模板；为 {@code null} 时回退到注解。 */
    public String getUserTemplateOverride() {
        return userTemplateOverride;
    }

    /** 动态会话 id（覆盖 {@code @MemoryId}）的供应器；为 {@code null} 时回退到注解 / 参数下标。 */
    public java.util.function.Supplier<String> getMemoryIdOverride() {
        return memoryIdOverride;
    }

    /** 会话 id 绑定的方法参数下标；为 {@code null} 时回退到注解 / 供应器。 */
    public Integer getMemoryIdParamIndex() {
        return memoryIdParamIndex;
    }

    /** 变量名 -> 方法参数下标的覆盖（替代 {@code @V}）。 */
    public Map<String, Integer> getVarOverrides() {
        return varOverrides;
    }

    /** 动态自定义变量的供应器集合，渲染模板时实时计算并入。 */
    public Map<String, java.util.function.Supplier<Object>> getCustomVariables() {
        return customVariables;
    }
}
