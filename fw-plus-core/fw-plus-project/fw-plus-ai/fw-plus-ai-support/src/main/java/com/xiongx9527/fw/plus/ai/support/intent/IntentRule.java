package com.xiongx9527.fw.plus.ai.support.intent;

import com.xiongx9527.fw.plus.ai.base.intent.IntentContext;

/**
 * 意图规则：规则引擎 / 规范 接入意图分类的最小自定义单元。
 *
 * <p>使用者有两种扩展方式：
 * <ul>
 *   <li>实现本接口（例如包装既有规则引擎的一行判定），交给 {@link RuleIntentClassifier} 统一求值；</li>
 *   <li>或直接用 {@link IntentRuleSpec} 以声明式“规范”描述一条规则（ref + 条件 + 分值）。</li>
 * </ul>
 * 规则按 {@link #getScore()} 降序参与最终排序，分值相同则先定义者优先。</p>
 */
public interface IntentRule {

    /** 命中后映射到哪个意图引用（通常对应某个技能 / 流程）。 */
    String getRef();

    /** 命中得分，用于与正则 / 向量 / LLM 分类结果在 {@code HybridIntentClassifier} 中合并排序。 */
    double getScore();

    /** 规则可读描述（规范文本），便于排障与可视化。 */
    String getDescription();

    /** 基于上下文判断是否命中。 */
    boolean test(IntentContext ctx);
}
