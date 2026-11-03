package com.xiongx9527.fw.plus.ai.askdata.capability;

/** 输入缺失（§6.38.6：缺 schemaContext 等显式输入时明确报错）。 */
public class MissingInputException extends SqlGenerationException {
    public MissingInputException(String message) {
        super(message);
    }

    /** 按状态码国际化构造（消息模板见 i18n/messages，{0} 为原始 reason）。 */
    public MissingInputException(int code, Object... args) {
        super(code, args);
    }
}
