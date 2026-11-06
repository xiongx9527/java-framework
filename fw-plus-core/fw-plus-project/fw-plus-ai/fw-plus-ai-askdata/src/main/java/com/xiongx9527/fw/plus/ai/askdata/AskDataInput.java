package com.xiongx9527.fw.plus.ai.askdata;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.support.retrieval.IndexMode;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * 端到端数据问答编排（召回 -> 生成 -> 执行）的显式入参。
 *
 * <p>所有字段不可变（final）；可选字段用 @Builder.Default 给定安全默认值。recallIndexMode 控制
 * 召回阶段走质量型（HIGH_QUALITY）还是经济型（ECONOMICAL）索引，详见 support 的 IndexMode。</p>
 */
@Getter
@Builder
public class AskDataInput {

    /** 用户自然语言问题（必填）。 */
    private final String question;

    /** SQL 方言提示（如 mysql / postgres），通常来自 DatasourceType.sqlDialectHint()。 */
    private final String dialectHint;

    /** 上一轮 SQL（P0 #12 / 6.9 多轮修正场景），用于增量改写。 */
    private final String priorSql;

    /** 引用消解结果（如把「上个月」解析成具体日期区间）。 */
    private final String refResolution;

    /** 额外业务约束（如“只看最近 7 天”），默认空列表。 */
    @lombok.Builder.Default
    private final List<String> constraints = Collections.emptyList();

    /** 采样种子，null 表示不固定。 */
    private final Integer seed;

    /** 采样温度，null 表示使用引擎默认。 */
    private final Double temperature;

    /** 核采样 top-p，null 表示使用引擎默认。 */
    private final Double topP;

    /** 透传给底层执行器的额外参数。 */
    @lombok.Builder.Default
    private final Map<String, Object> extraParams = Collections.emptyMap();

    /** 最大返回行数，防止大结果集拖垮内存；<=0 表示不限制。 */
    @lombok.Builder.Default
    private final int maxRows = 1000;

    /** 查询超时（秒），<=0 表示不限制。 */
    @lombok.Builder.Default
    private final int queryTimeoutSeconds = 30;

    /** 是否只读执行；为 true 时执行器应拒绝写入型语句。 */
    @lombok.Builder.Default
    private final boolean readOnly = true;

    /** 召回索引策略：质量型（HIGH_QUALITY）或经济型（ECONOMICAL），默认质量型。 */
    @lombok.Builder.Default
    private final IndexMode recallIndexMode = IndexMode.HIGH_QUALITY;

    /** 多轮对话历史（可选），由 ChatMemory 或调用方传入，用于保持上下文（§6.38 多轮记忆）。 */
    @lombok.Builder.Default
    private final List<ChatMessage> history = Collections.emptyList();
}
