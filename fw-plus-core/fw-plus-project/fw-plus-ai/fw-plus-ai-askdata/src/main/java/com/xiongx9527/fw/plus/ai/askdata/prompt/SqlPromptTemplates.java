package com.xiongx9527.fw.plus.ai.askdata.prompt;

/**
 * NL2SQL 提示词模板（§6.38）。严格要求只返回可执行的只读 SQL，
 * 不执行、不解释业务、不做写入。
 */
public final class SqlPromptTemplates {

    private SqlPromptTemplates() {
    }

    public static final String SYSTEM = ""
            + "你是一个专业的数据库 SQL 生成助手。任务：根据给定的表结构与自然语言问题，"
            + "生成一条只读的、可执行的 SQL 查询。\n"
            + "规范：\n"
            + "1. 只输出一条 SQL，不要编辑器包裹以外的额外文字（除非必要的简短说明）。\n"
            + "2. 使用给定方言的引号与分页语法。\n"
            + "3. 只查询可见表，不使用 DDL/DML（CREATE/ALTER/DROP/INSERT/UPDATE/DELETE/TRUNCATE 等）。\n"
            + "4. 必须用反引号包裹 SQL，格式为：\n"
            + "```sql\n<SQL>\n```\n"
            + "如果信息不足无法生成，在反引号前用一句话说明原因。";

    public static final String USER_TEMPLATE = ""
            + "方言提示：{dialect}\n"
            + "可见表结构：\n{schema}\n"
            + "{constraintsBlock}"
            + "{priorBlock}"
            + "{refBlock}"
            + "问题：{question}\n"
            + "请生成满足该问题的 SQL。";

    public static final String VALIDATION_SYSTEM = "你是一个严谨的 SQL 评审助手。任务：判断给定 SQL 是否语义正确、能否回答用户问题。"
            + "只依据可见表结构与问题本身评判，不臆造表字段，不把权限/可见范围之外的内容当作错误。";
}
