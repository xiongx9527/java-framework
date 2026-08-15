package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityDescriptor;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityProvider;
import com.xiongx9527.fw.plus.ai.askdata.capability.AskDataCapabilityType;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataContextCompressionRequest;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataContextCompressionResult;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.model.AiChatModel;
import com.xiongx9527.fw.plus.ai.model.AiChatRequest;
import com.xiongx9527.fw.plus.ai.model.AiChatResult;

import java.util.Collections;

/**
 * 使用业务已选择的 LangChain4j Chat 模型生成脱敏语义摘要。
 *
 * <p>本 Adapter 不创建模型、不选择 Provider，也不读取凭据。模型实例由业务通过现有模型注册表提供；
 * 结构化恢复字段不进入模型输出解析，始终从请求原样复制到结果。</p>
 */
public final class LangChain4jAskDataContextCompressor
		implements AskDataContextCompressor, AskDataCapabilityProvider {

	private final String key;
	private final AiChatModel chatModel;

	public LangChain4jAskDataContextCompressor(String key, AiChatModel chatModel) {
		if (key == null || key.trim().length() == 0 || chatModel == null) {
			throw new IllegalArgumentException("key 和 chatModel 不能为空");
		}
		this.key = key.trim();
		this.chatModel = chatModel;
	}

	@Override
	public AskDataContextCompressionResult compress(AskDataContextCompressionRequest request) {
		if (request == null) { throw new IllegalArgumentException("request 不能为空"); }
		String prompt = "Summarize the following redacted AskData execution context. "
				+ "Do not invent SQL, permissions, identifiers or credentials.\n" + request.getRedactedContext();
		AiChatResult result = chatModel.chat(new AiChatRequest(prompt, Collections.<String, Object>emptyMap(),
				request.getTimeoutMillis()));
		if (result == null || result.getText().trim().length() == 0) {
			throw new IllegalStateException("LangChain4j 语义压缩未返回摘要");
		}
		return new AskDataContextCompressionResult(result.getText(), result.getActualModelKey(),
				request.getStructuredFields());
	}

	@Override
	public AskDataCapabilityDescriptor getDescriptor() {
		return new AskDataCapabilityDescriptor(key, AskDataCapabilityType.RUNTIME_ADAPTER, "0.1.1-SNAPSHOT",
				"0.1.1", "0.1.1", 0, true, "fw.plus.ai.askdata.compression.langchain4j");
	}

	@Override public Object getCapability() { return this; }
}
