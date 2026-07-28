package com.xiongx9527.fw.plus.ai.askdata.model;

/** Core 无关的对话模型 SPI。 */
public interface AskDataChatModel {
	AskDataChatResult chat(AskDataChatRequest request);
}
