package com.xiongx9527.fw.plus.ai.interactive.confirm;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * 人工确认失败：token 已用 / 已过期 / 动作摘要不一致（防止"确认 A、提交 B"）。
 */
public class ConfirmationException extends BizException {

    private static final long serialVersionUID = 1L;

    /** 状态码：服务端 + 确认层 + 006。 */
    public static final int CODE = 50_008_006;

    public ConfirmationException(String message) {
        super(CODE, message);
    }

    public ConfirmationException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
