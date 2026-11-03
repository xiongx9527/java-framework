package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.common.exception.BizException;

/** SQL 生成过程中的业务异常（LLM 调用、解析、校验失败等），继承 {@link BizException} 以便统一状态码与 i18n。 */
public class SqlGenerationException extends BizException {
    public SqlGenerationException(String message) {
        super(message);
    }

    public SqlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }

    /** 按状态码国际化构造（消息模板见 i18n/messages，{0} 为原始 reason）。 */
    public SqlGenerationException(int code, Object... args) {
        super(code, args);
    }

    /** 按状态码 + 已解析文案 + 原始异常构造。 */
    public SqlGenerationException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
