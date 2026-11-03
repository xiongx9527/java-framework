package com.xiongx9527.fw.plus.ai.base.guard;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * 出站地址被拒绝（协议不允许或指向内网 / 元数据服务）。
 */
public class OutboundUrlBlockedException extends BizException {

    private static final long serialVersionUID = 1L;

    /** 状态码：服务端 + 安全/风控 + 001。 */
    public static final int CODE = 50_020_001;

    public OutboundUrlBlockedException(String message) {
        super(CODE, message);
    }
}
