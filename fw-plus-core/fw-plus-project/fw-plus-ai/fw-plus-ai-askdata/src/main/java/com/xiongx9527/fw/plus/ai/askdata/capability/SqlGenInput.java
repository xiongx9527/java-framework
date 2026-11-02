package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * SQL 生成能力的显式输入（全部显式，无隐式全局状态，见 §6.38.2）。
 */
@Getter
@Builder
public class SqlGenInput {
    /** 已按 §6.22 归一、锚定相对时间的问题。 */
    private final String question;

    /** 召回的表 / 字段 / 关系（可见 schema）。缺失应报错（§6.38.6）。 */
    private final SchemaContext schemaContext;

    /** 方言提示字符串（产品侧 DatasourceType.sqlDialectHint() 映射）。 */
    private final String dialectHint;

    /** 多轮指代消解结果（P0 #12 / §6.9），可选。 */
    private final String priorSql;

    /** 指代消解说明，可选。 */
    private final String refResolution;

    /** 上游注入的行 / 列级约束（仅消费，不再判权限）。 */
    @Builder.Default
    private final List<String> constraints = Collections.emptyList();

    /** 输入合法性自检：问题与可见 schema 均不可缺。 */
    public void validate() {
        if (question == null || question.trim().isEmpty()) {
            throw new MissingInputException("question 不可为空");
        }
        if (schemaContext == null || schemaContext.isEmpty()) {
            throw new MissingInputException("schemaContext 不可为空（SQL 生成依赖可见 schema，不依赖隐式全局状态）");
        }
    }
}
