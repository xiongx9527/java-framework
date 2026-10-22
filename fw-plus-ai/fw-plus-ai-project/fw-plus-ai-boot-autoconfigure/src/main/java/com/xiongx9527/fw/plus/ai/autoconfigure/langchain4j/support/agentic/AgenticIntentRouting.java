package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentClassifier;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent.IntentSource;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.agentic.scope.AgenticScope;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 把意图分类接到 agentic 条件编排（{@link AgenticWorkflows#conditional()}）的桥接器：
 * 让「按意图选分支」在<b>一次编排内只判一次意图</b>。
 *
 * <p>背景：langchain4j-agentic 的 {@code ConditionalPlanner.firstAction} 会对每个分支各求值一次
 * 条件（{@code conditionalSubagents.stream().filter(...)} 不短路），且在分支执行前一次性求完。
 * 若把 {@code classifier.classify(...)} 直接写进每个 {@link Predicate}，N 个分支就会判 N 次
 * （大模型层级即 N 次模型调用）。本类提供两种避免重复判定的用法：</p>
 *
 * <ol>
 *   <li><b>记忆化条件</b>：{@link #is(Enum)} / {@link #in(Enum...)} 返回的条件共享同一份判定结果——
 *       首个被求值的条件判定一次并写回 {@link AgenticScope}，其余条件读缓存。条件求值在
 *       {@code ConditionalPlanner} 中是顺序、同 scope、先全部求值完再执行分支，故记忆化可靠。</li>
 *   <li><b>边界判定 + 注入输入</b>：{@link #inputs(String)} 在编排外判定一次，返回可直接喂给
 *       {@code UntypedAgent#invoke(Map)} 的输入 Map（含原始请求与意图名）；配合 {@code is(...)} /
 *       {@code in(...)} 使用时，条件优先读注入的意图，编排内不再判定。</li>
 * </ol>
 *
 * <p>本类是 {@code agentic} 向 {@code intent} 的有意桥接：{@code AgenticScope} 属 langchain4j-agentic
 * 实验 API，必须收口在本（agentic）包内，故桥接类放在这里而非 {@code intent} 包。</p>
 *
 * <p>用法示例：</p>
 * <pre>{@code
 * AgenticIntentRouting<MyIntent> routing = AgenticIntentRouting.of(classifier);
 *
 * // 形态一：编排内记忆化，一次判定
 * UntypedAgent a = AgenticWorkflows.conditional()
 *         .subAgents(routing.is(MyIntent.LOGISTICS), logisticsAgent)
 *         .subAgents(routing.is(MyIntent.REFUND), refundAgent)
 *         .outputKey("answer")
 *         .build();
 * a.invoke(Map.of("request", "我的包裹到哪了"));
 *
 * // 形态二：边界判定一次，注入输入，编排内零判定
 * a.invoke(routing.inputs("我的包裹到哪了"));
 * }</pre>
 *
 * <p>默认从 scope 的 {@code request} 键读取待判定文本、把意图名写入 {@code intent} 键，
 * 可经 {@link #textKey(String)} / {@link #intentKey(String)} 调整。</p>
 *
 * @param <I> 使用方自定义的意图枚举类型
 */
public final class AgenticIntentRouting<I extends Enum<I>> {

    /** 记忆化键：存放本类写入的判定结果，避免与业务 state 冲突。 */
    private static final String MEMO_KEY = "__fwPlusIntent_judged";

    /** 未匹配哨兵：非空白字符串，确保写入后 {@code hasState} 为真、且不等于任何意图名。 */
    private static final String NONE = "__fwPlusIntent_none";

    private static final String DEFAULT_TEXT_KEY = "request";

    private static final String DEFAULT_INTENT_KEY = "intent";

    private final IntentClassifier<I> classifier;

    private final String textKey;

    private final String intentKey;

    private AgenticIntentRouting(IntentClassifier<I> classifier, String textKey, String intentKey) {
        this.classifier = classifier;
        this.textKey = textKey;
        this.intentKey = intentKey;
    }

    /** 以分类器构建（默认文本键 {@code request}、意图键 {@code intent}）。 */
    public static <I extends Enum<I>> AgenticIntentRouting<I> of(IntentClassifier<I> classifier) {
        if (classifier == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
                    "fw.ai.AgenticIntentRouting.classifierRequired"));
        }
        return new AgenticIntentRouting<I>(classifier, DEFAULT_TEXT_KEY, DEFAULT_INTENT_KEY);
    }

    /** 待判定文本在 scope 中的键（默认 {@code request}）。 */
    public AgenticIntentRouting<I> textKey(String textKey) {
        return new AgenticIntentRouting<I>(classifier, requireKey(textKey, "textKey"), intentKey);
    }

    /** 注入/读取意图名在 scope 中的键（默认 {@code intent}）。 */
    public AgenticIntentRouting<I> intentKey(String intentKey) {
        return new AgenticIntentRouting<I>(classifier, textKey, requireKey(intentKey, "intentKey"));
    }

    /** 条件谓词：当前意图等于指定值。共享一次判定（读取注入值或记忆化判定）。 */
    public Predicate<AgenticScope> is(I intent) {
        if (intent == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
                    "fw.ai.AgenticIntentRouting.intentRequired"));
        }
        return scope -> intent.name().equals(resolvedIntentName(scope));
    }

    /** 条件谓词：当前意图命中给定集合中任一值。共享一次判定。 */
    public Predicate<AgenticScope> in(I... intents) {
        Set<String> names = new LinkedHashSet<String>();
        if (intents != null) {
            for (I intent : intents) {
                if (intent != null) {
                    names.add(intent.name());
                }
            }
        }
        return scope -> names.contains(resolvedIntentName(scope));
    }

    /**
     * 在编排外判定一次，返回可直接喂给 {@code UntypedAgent#invoke(Map)} 的输入 Map：
     * 含原始请求（{@link #textKey(String)} 键）与意图名（{@link #intentKey(String)} 键，
     * 未命中为哨兵值，条件据此判为不匹配）。
     */
    public Map<String, Object> inputs(String request) {
        Map<String, Object> inputs = new LinkedHashMap<String, Object>();
        inputs.put(textKey, request);
        inputs.put(intentKey, resolvedIntentName(judge(request)));
        return inputs;
    }

    /** 当前 scope 对应的意图名：优先读注入值，缺省时按需判定并记忆化。未命中返回 {@code null}。 */
    private String resolvedIntentName(AgenticScope scope) {
        if (scope.hasState(intentKey)) {
            Object injected = scope.readState(intentKey, null);
            return injected == null ? null : String.valueOf(injected);
        }
        return resolvedIntentName(judgeAndMemoize(scope));
    }

    /** 记忆化判定：首次求值时判定一次并写回 scope，后续调用读缓存。 */
    @SuppressWarnings("unchecked")
    private IntentResult<I> judgeAndMemoize(AgenticScope scope) {
        Object cached = scope.readState(MEMO_KEY, null);
        if (cached instanceof IntentResult) {
            return (IntentResult<I>) cached;
        }
        IntentResult<I> result = judge(scope.readState(textKey, ""));
        scope.writeStateIfAbsent(MEMO_KEY, result);
        return result;
    }

    /** 执行一次判定，空结果兜底为 {@link IntentSource#FALLBACK}。 */
    private IntentResult<I> judge(String text) {
        IntentResult<I> result = classifier.classify(IntentContext.of(text));
        return result == null ? IntentResult.none(IntentSource.FALLBACK) : result;
    }

    /** 从判定结果取意图名；未匹配返回哨兵 {@link #NONE}（非空白，保证 hasState 语义）。 */
    private String resolvedIntentName(IntentResult<I> result) {
        I intent = result == null ? null : result.intent();
        return intent == null ? NONE : intent.name();
    }

    private static String requireKey(String key, String name) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
                    "fw.ai.AgenticIntentRouting.keyRequired", name));
        }
        return key;
    }
}
