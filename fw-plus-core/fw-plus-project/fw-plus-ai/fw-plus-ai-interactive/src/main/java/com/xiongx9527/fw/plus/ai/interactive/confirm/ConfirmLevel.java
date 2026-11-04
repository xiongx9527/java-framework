package com.xiongx9527.fw.plus.ai.interactive.confirm;

/**
 * 人工确认级别（对应文档 §6.33.2 的四级确认）。
 *
 * <p>级别由 {@link ConfirmGate} 按"动作类型 × 影响行数 × 是否可逆"规则判定，不由模型判断。</p>
 */
public enum ConfirmLevel {

    /** L0 无确认：直接执行（只读查询、小结果集导出）。 */
    NONE,
    /** L1 轻确认：一个按钮（"继续导出"）。 */
    LIGHT,
    /** L2 复述确认：展示将要执行的具体动作与参数，用户核对后点确认（预约提交、删除单条）。 */
    REPEAT,
    /** L3 双人确认：第二个人审批（删数据源、改权限、批量删除）。 */
    DUAL
}
