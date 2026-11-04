package com.xiongx9527.fw.plus.ai.interactive;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * 交互式收集（反问 / 选项 / 问卷 / 预约）过程中的异常：缺参判定、答案校验失败、提取不确定等。
 *
 * <p>区别于普通业务异常：此类异常由交互框架自身抛出，用于驱动"重新反问"或"降级取默认值"，
 * 而不是终止整个对话。</p>
 */
public class InteractiveException extends BizException {

    private static final long serialVersionUID = 1L;

    /** 状态码：服务端 + 交互层 + 005。 */
    public static final int CODE = 50_008_005;

    public InteractiveException(String message) {
        super(CODE, message);
    }

    public InteractiveException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
