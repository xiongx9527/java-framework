package dev.langchain4j.store.graph.nebula;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * NebulaGraph 操作异常。
 */
public class NebulaException extends BizException {

    /** 状态码 50009008（SERVER/DATABASE/008 NebulaGraph 操作异常） */
    public static final int CODE = 50_009_008;

    public NebulaException(String message) {

        super(CODE, message);
    }

    public NebulaException(String message, Throwable cause) {

        super(CODE, message, cause);
    }
}
