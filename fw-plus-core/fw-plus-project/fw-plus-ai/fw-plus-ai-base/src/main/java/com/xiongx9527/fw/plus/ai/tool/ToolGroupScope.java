package com.xiongx9527.fw.plus.ai.tool;

/**
 * 工具组作用域。
 *
 * <p><b>作用</b>：区分「元工具组」（框架自身提供给 Agent 的元能力，如技能装载、计划管理等）
 * 与「外部工具组」（业务/第三方提供的真实工具）。作用域决定工具组在提示词与权限策略中的归类方式，
 * 避免元能力被业务侧误禁用。</p>
 *
 * <p><b>出处</b>：B 类（AgentScope 特殊抽象）。镜像 {@code io.agentscope.core.tool.ToolGroupScope}
 * （agentscope-core 2.0.4）。</p>
 *
 * @author xiongxiao
 */
public enum ToolGroupScope {

    /** 元能力工具组：框架自身提供，支撑 Agent 运行时行为。 */
    META,

    /** 外部工具组：业务或第三方接入的真实工具。 */
    EXTERNAL
}
