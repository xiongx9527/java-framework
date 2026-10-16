package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.session;

import java.io.Serializable;

public interface SessionIdResolver extends Serializable {

	String resolve(SessionContext ctx);

}