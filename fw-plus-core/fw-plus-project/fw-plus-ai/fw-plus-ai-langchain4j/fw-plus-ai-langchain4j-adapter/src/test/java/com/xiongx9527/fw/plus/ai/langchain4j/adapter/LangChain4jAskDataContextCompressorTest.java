package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataContextCompressionRequest;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataContextCompressionResult;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.model.AiChatModel;
import com.xiongx9527.fw.plus.ai.model.AiChatRequest;
import com.xiongx9527.fw.plus.ai.model.AiChatResult;
import com.xiongx9527.fw.plus.ai.model.AiModelUsage;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LangChain4jAskDataContextCompressorTest {

	@Test
	public void shouldPreserveStructuredRecoveryFieldsOutsideModelOutput() {
		AiChatModel model = new AiChatModel() {
			@Override public AiChatResult chat(AiChatRequest request) {
				return new AiChatResult("semantic summary", "business-model", "STOP", AiModelUsage.unknown());
			}
			@Override public String getKey() { return "business-model"; }
			@Override public String getFramework() { return "langchain4j"; }
			@Override public String getProvider() { return "business"; }
			@Override public com.xiongx9527.fw.plus.ai.model.AiModelType getType() {
				return com.xiongx9527.fw.plus.ai.model.AiModelType.CHAT;
			}
		};
		LangChain4jAskDataContextCompressor compressor =
				new LangChain4jAskDataContextCompressor("langchain4j-summary", model);

		AskDataContextCompressionResult result = compressor.compress(new AskDataContextCompressionRequest(
				new AskDataProjectContext("project-a", "operator-a"), "redacted context",
				Collections.singletonMap("authorizationVersion", "policy-v1"), 30000L));

		assertEquals("semantic summary", result.getSummary());
		assertEquals("business-model", result.getActualModelKey());
		assertEquals("policy-v1", result.getStructuredFields().get("authorizationVersion"));
		assertEquals("fw.plus.ai.askdata.compression.langchain4j",
				compressor.getDescriptor().getConfigurationNamespace());
	}
}
