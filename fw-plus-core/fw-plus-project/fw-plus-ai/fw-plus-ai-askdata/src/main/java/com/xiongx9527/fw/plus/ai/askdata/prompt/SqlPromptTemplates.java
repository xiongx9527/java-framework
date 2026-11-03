package com.xiongx9527.fw.plus.ai.askdata.prompt;

/**
 * NL2SQL 提示词模板（§6.38）。严格要求只返回可执行的只读 SQL，
 * 不执行、不解释业务、不做写入。
 */
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;

/**
 * NL2SQL 提示词模板（§6.38）。严格要求只返回可执行的只读 SQL，
 * 不执行、不解释业务、不做写入。文案来自 i18n 资源（askdata.prompt.sql.*），
 * 模板中的 {dialect}/{schema}/{question} 等为发送给 LLM 的占位符，运行时由调用方 replace。
 */
public final class SqlPromptTemplates {

    private SqlPromptTemplates() {
    }

    public static final String SYSTEM = I18nMessageResolver.resolveKey("askdata.prompt.sql.system");

    public static final String USER_TEMPLATE = I18nMessageResolver.resolveKey("askdata.prompt.sql.user");

    public static final String VALIDATION_SYSTEM = I18nMessageResolver.resolveKey("askdata.prompt.sql.validation_system");
}
