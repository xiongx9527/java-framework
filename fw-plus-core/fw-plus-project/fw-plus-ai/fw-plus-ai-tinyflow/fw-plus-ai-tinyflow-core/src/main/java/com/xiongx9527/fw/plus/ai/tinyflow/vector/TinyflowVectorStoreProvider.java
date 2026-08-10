package com.xiongx9527.fw.plus.ai.tinyflow.vector;

import java.util.List;

public interface TinyflowVectorStoreProvider {

	List<TinyflowVectorStoreDescriptor> list();

	TinyflowVectorStore get(String key);
}
