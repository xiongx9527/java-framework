package com.xiongx9527.fw.plus.ai.askdata.vector;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

/** 向量嵌入 SPI。 */
public interface AskDataEmbeddingModel {
	float[] embed(AskDataProjectContext context, String text);
}
