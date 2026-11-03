package com.xiongx9527.fw.plus.ai.base.intent;

import java.util.List;

/**
 * 意图分类端口：把用户文本映射到业务意图（通常就是某个技能 / 流程的引用）。
 *
 * <p>具体策略（正则 / 向量 / LLM / 混合）属于组合层，由 support 提供；base 只定义中性契约，
 * 不绑定任何分类实现。</p>
 */
public interface IntentPort {

    IntentResult classify(IntentRequest request);
}
