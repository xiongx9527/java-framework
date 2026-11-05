package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * SQL 生成能力的显式入参（6.38.2 生成节点）。
 *
 * <p>与 AskDataInput 不同，这里直接使用已解析的 SchemaContext，避免生成阶段再次做表发现。
 * validate() 在生成前校验必填项，缺失时抛出 MissingInputException（带 i18n key）。</p>
 */
@Getter
@Builder
public class SqlGenInput {

    /** 用户自然语言问题（必填）。 */
    private final String question;

    /** 已解析的库表 schema；生成阶段据此决定可引用的表/列（必填）。 */
    private final SchemaContext schemaContext;

    /** SQL 方言提示（如 mysql / postgres），来自 DatasourceType.sqlDialectHint()。 */
    private final String dialectHint;

    /** 上一轮 SQL（P0 #12 / 6.9 多轮修正），用于增量改写。 */
    private final String priorSql;

    /** 引用消解结果。 */
    private final String refResolution;

    /** 额外业务约束，默认空列表。 */
    @lombok.Builder.Default
    private final List<String> constraints = Collections.emptyList();

    /** 采样种子，null 表示不固定。 */
    private final Integer seed;

    /** 采样温度，null 表示使用引擎默认。 */
    private final Double temperature;

    /** 核采样 top-p，null 表示使用引擎默认。 */
    private final Double topP;

    /** 透传额外参数。 */
    @lombok.Builder.Default
    private final Map<String, Object> extraParams = Collections.emptyMap();

    /** 生成前校验：问题与 schema 均不可为空。 */
    public void validate() {
        if (question == null || question.trim().isEmpty()) {
            throw new MissingInputException(50005804, I18nMessageResolver.resolveKey("askdata.input.question_required"));
        }
        if (schemaContext == null || schemaContext.isEmpty()) {
            throw new MissingInputException(50005804, I18nMessageResolver.resolveKey("askdata.input.schema_required"));
        }
    }
}
