package com.xiongx9527.fw.plus.ai.base.model.moderation;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * 审核拒绝：调用方要求「命中即拒绝」时抛出。
 */
public class ModerationException extends BizException {

    private static final long serialVersionUID = 1L;

    /** 状态码：客户端 + 业务 + 001。 */
    public static final int CODE = 40_005_001;

    private final transient ModerationResult result;

    public ModerationException(ModerationResult result) {
        super(CODE, result == null || result.getReason() == null
                ? "content rejected by moderation" : result.getReason());
        this.result = result;
    }

    public ModerationResult getResult() {
        return result;
    }
}
