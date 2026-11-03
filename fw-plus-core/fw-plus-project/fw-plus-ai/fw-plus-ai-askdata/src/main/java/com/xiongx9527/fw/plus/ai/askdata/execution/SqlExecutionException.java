package com.xiongx9527.fw.plus.ai.askdata.execution;

import com.xiongx9527.fw.plus.common.exception.BizException;

/** SQL 执行异常（连接 / 语法 / 超时等），继承 {@link BizException} 以便统一状态码与 i18n。 */
public class SqlExecutionException extends BizException {
    public SqlExecutionException(String message) {
        super(message);
    }

    public SqlExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    /** 按状态码国际化构造（消息模板见 i18n/messages，{0} 为原始 reason）。 */
    public SqlExecutionException(int code, Object... args) {
        super(code, args);
    }

    /** 按状态码 + 已解析文案 + 原始异常构造。 */
    public SqlExecutionException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
