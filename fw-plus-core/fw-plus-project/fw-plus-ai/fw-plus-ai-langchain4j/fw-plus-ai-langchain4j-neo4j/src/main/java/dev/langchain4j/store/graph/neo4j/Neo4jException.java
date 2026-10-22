package dev.langchain4j.store.graph.neo4j;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class Neo4jException extends BizException {

    /** 状态码 50009007（SERVER/DATABASE/007 Neo4j 图操作异常） */
    public static final int CODE = 50_009_007;

    public Neo4jException(String message, Throwable cause) {

        super(CODE, message, cause);
    }
}
