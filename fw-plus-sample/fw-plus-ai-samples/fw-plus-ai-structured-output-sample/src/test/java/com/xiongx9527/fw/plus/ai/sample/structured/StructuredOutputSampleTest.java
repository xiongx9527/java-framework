package com.xiongx9527.fw.plus.ai.sample.structured;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutput;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 样例自检：证明样例本身可跑（离线，不需要模型与 Key），并钉住对外宣称的用法。
 */
class StructuredOutputSampleTest {

	private static final String GOOD = "{\"orderId\":\"A1001\",\"quantity\":3,\"status\":\"paid\"}";

	@Test
	void mainRunsOffline() throws Exception {
		// 两个演示都不依赖模型/Key，跑通即证明样例可用
		StructuredOutputSample.main(new String[0]);
	}

	@Test
	void schemaFromResourcesDrivesInstructionAndValidation() throws Exception {
		StructuredOutput structuredOutput = StructuredOutputSample.orderExtractor();

		assertThat(structuredOutput.instruction()).contains("只输出 JSON").contains("orderId");
		assertThat(structuredOutput.validate(JSONObject.parseObject(GOOD)).isValid()).isTrue();
		// quantity=0：schema 认（合法 integer），业务规则拦下（paid 时必须 > 0）
		assertThat(structuredOutput.validate(JSONObject.parseObject("{\"orderId\":\"A1\",\"quantity\":0,\"status\":\"paid\"}"))
				.errors()).isNotEmpty();
	}

	@Test
	void retryLoopFeedsErrorsBack() throws Exception {
		StructuredOutput structuredOutput = StructuredOutputSample.orderExtractor();
		final List<String> prompts = new ArrayList<String>();

		JSONObject order = structuredOutput.chat(prompt -> {
			prompts.add(prompt);
			return prompts.size() == 1 ? "{\"quantity\":3}" : GOOD;
		}, "把订单信息抽出来", 3);

		assertThat(order.getString("orderId")).isEqualTo("A1001");
		assertThat(prompts).hasSize(2);
		assertThat(prompts.get(1)).contains("orderId");   // 上一轮的错误被回喂进提示词
	}

	@Test
	void wiringWithFrameworkAssistantWorks() throws Exception {
		// 用假模型走完整链路：AssistantBuilder → SimpleAssistant → StructuredOutput.chat(...)
		ChatModel fakeModel = new ChatModel() {

			@Override
			public ChatResponse doChat(ChatRequest request) {
				return ChatResponse.builder().aiMessage(AiMessage.from(GOOD)).build();
			}
		};

		JSONObject order = StructuredOutputSample.extractWith(fakeModel, "sample-1", "把订单信息抽出来");

		assertThat(order.getString("orderId")).isEqualTo("A1001");
		assertThat(order.getIntValue("quantity")).isEqualTo(3);
	}
}
