package dev.langchain4j.store.graph.nebula;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * NebulaGraph 操作异常。
 */
public class NebulaException extends BizException {

    public NebulaException(String message) {

        super(message);
    }

    public NebulaException(String message, Throwable cause) {

        super(message, cause);
    }
}
