package com.xiongx9527.fw.plus.ai.base.tool;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * 工具执行失败（工具不存在、参数不匹配、方法内部抛错）。
 */
public class ToolExecutionException extends BizException {

    private static final long serialVersionUID = 1L;

    /** 状态码：服务端 + 第三方服务 + 002。 */
    public static final int CODE = 50_008_002;

    public ToolExecutionException(String message) {
        super(CODE, message);
    }

    public ToolExecutionException(String message, Throwable cause) {
        super(CODE, message, cause);
    }
}
