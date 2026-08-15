package com.xiongx9527.fw.plus.ai.model;

public interface AiChatModel extends AiModel {

	AiChatResult chat(AiChatRequest request);
}
