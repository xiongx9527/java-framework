package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import java.util.List;

public interface AskDataEmbeddingModel {

	List<Float> embed(String text);
}
