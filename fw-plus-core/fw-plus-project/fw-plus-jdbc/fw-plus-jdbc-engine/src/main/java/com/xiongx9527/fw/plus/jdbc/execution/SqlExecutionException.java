package com.xiongx9527.fw.plus.jdbc.execution;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * SQL 执行异常（连接 / 语法 / 超时 / 约束等），继承 {@link BizException} 以便统一状态码与 i18n。
 *
 * <p>新增 {@link #getCategory()}：执行器会按 {@link SqlErrorClassifier} 推断分类后构造，
 * 产品侧可据此给出差异化提示（如“查询超时，请缩小范围” vs “SQL 语法错误”）。</p>
 */
public class SqlExecutionException extends BizException {

    private final SqlErrorCategory category;

    public SqlExecutionException(String message) {
        super(message);
        this.category = SqlErrorCategory.UNKNOWN;
    }

    public SqlExecutionException(String message, Throwable cause) {
        super(message, cause);
        this.category = SqlErrorCategory.UNKNOWN;
    }

    /** 按状态码国际化构造（消息模板见 i18n/messages，{0} 为原始 reason）。 */
    public SqlExecutionException(int code, Object... args) {
        super(code, args);
        this.category = SqlErrorCategory.UNKNOWN;
    }

    /** 按状态码 + 已解析文案 + 原始异常构造。 */
    public SqlExecutionException(int code, String message, Throwable cause) {
        super(code, message, cause);
        this.category = SqlErrorCategory.UNKNOWN;
    }

    /** 按状态码 + 错误分类 + 已解析文案 + 原始异常构造（执行器分类后使用）。 */
    public SqlExecutionException(int code, SqlErrorCategory category, String message, Throwable cause) {
        super(code, message, cause);
        this.category = category == null ? SqlErrorCategory.UNKNOWN : category;
    }

    /** 错误分类；永不返回 null。 */
    public SqlErrorCategory getCategory() {
        return category == null ? SqlErrorCategory.UNKNOWN : category;
    }
}
