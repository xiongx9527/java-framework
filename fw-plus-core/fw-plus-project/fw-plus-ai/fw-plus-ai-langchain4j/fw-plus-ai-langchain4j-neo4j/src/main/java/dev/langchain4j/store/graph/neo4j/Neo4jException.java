package dev.langchain4j.store.graph.neo4j;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class Neo4jException extends BizException {

    public Neo4jException(String message, Throwable cause) {

        super(message, cause);
    }
}
