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
}
