package com.xiongx9527.fw.plus.ai.askdata.answer;

import com.xiongx9527.fw.plus.jdbc.execution.QueryResult;
import lombok.Builder;
import lombok.Getter;

/**
 * 自然语言答案合成的输入：原始问题 + 已生成 SQL + 已执行结果集。
 */
@Getter
@Builder
public class AnswerInput {

    /** 用户的自然语言问题。 */
    private final String question;

    /** 本次执行的 SQL。 */
    private final String sql;

    /** 执行得到的结构化结果集（可能为空）。 */
    private final QueryResult queryResult;

    /** 方言提示，用于措辞（如“返回 N 行”）。 */
    private final String dialectHint;
}
