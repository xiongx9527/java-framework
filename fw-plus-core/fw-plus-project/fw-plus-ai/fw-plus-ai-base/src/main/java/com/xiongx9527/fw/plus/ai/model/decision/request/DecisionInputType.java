package com.xiongx9527.fw.plus.ai.model.decision.request;

/**
 * 决策输入类型。
 *
 * <p><b>作用</b>：区分决策问题所依据的原始材料形态（自由文本 / 结构化字段 / 多模态内容 / 对话消息），
 * 让适配层选择对应的提示词组装方式。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.request.DecisionInputType}。</p>
 *
 * @author xiongxiao
 */
public enum DecisionInputType {

    /** 自由文本输入。 */
    TEXT,

    /** 结构化字段输入。 */
    FIELDS,

    /** 多模态内容输入。 */
    CONTENTS,

    /** 对话消息输入。 */
    MESSAGES
}
