package com.xiongx9527.fw.plus.ai.tinyflow.node;

import dev.tinyflow.core.parser.NodeParser;

public interface TinyflowNodeProvider {

	String getType();

	NodeParser<?> getParser();

	TinyflowNodeDescriptor getDescriptor();

	int getOrder();
}
