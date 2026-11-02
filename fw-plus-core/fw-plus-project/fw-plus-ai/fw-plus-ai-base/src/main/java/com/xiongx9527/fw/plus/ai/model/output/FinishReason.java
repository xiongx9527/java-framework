package com.xiongx9527.fw.plus.ai.model.output;

/**
 * 模型生成终止原因。
 *
 * <p><b>作用</b>：标识一次生成为什么结束——正常结束、达到长度上限、触发工具调用、
 * 被内容策略过滤，或其它原因。上层可据此决定是否继续追加一轮生成、是否执行工具、是否提示用户。</p>
 *
 * <p><b>出处</b>：逐字镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.output.FinishReason}。
 * Spring AI 等价 {@code org.springframework.ai.chat.model.Generation} 的 finishReason 字符串。</p>
 *
 * <p><b>实现约定</b>：不要新增枚举值以外的语义；各厂商 SDK 的原始原因字符串由适配层映射到本枚举，
 * 无法映射时统一落到 {@link #OTHER}，不要抛异常。</p>
 */
public enum FinishReason {

    /** 模型正常结束生成。 */
    STOP,

    /** 因达到 max tokens 或上下文长度上限被截断。 */
    LENGTH,

    /** 模型要求执行工具调用，本轮不是最终答案。 */
    TOOL_EXECUTION,

    /** 输出被内容安全策略过滤。 */
    CONTENT_FILTER,

    /** 其它原因（厂商特有、未知或不可映射）。 */
    OTHER
}
