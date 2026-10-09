package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.ToolExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link GuardedToolExecutor}：0.35 下的工具参数错误拦截——
 * 缺参在执行前拦截（0.35 原生静默传 null）、绑定失败在执行中拦截，统一交给 handler 回文本给 LLM。
 */
class GuardedToolExecutorTest {

	static class OrderTool {
		boolean invoked;

		@Tool(name = "order", value = "下单")
		public String order(@P("商品编号") String productId, @P("数量") int quantity,
				@P(value = "备注", required = false) String remark) {
			invoked = true;
			return "OK:" + productId + ":" + quantity;
		}

		@Tool(name = "ping", value = "探活")
		public String ping() {
			invoked = true;
			return "pong";
		}

		@Tool(name = "risk", value = "风险操作")
		public String risk() {
			invoked = true;
			throw LlmVisibleToolExecutionException.from("库存不足，请让用户减少购买数量");
		}

		@Tool(name = "silentFail", value = "静默失败")
		public String silentFail() {
			invoked = true;
			throw new IllegalStateException();
		}
	}

	private final OrderTool tool = new OrderTool();

	private ToolSpecification spec(String name) {
		for (ToolSpecification specification : ToolSpecifications.toolSpecificationsFrom(tool)) {
			if (specification.name().equals(name)) {
				return specification;
			}
		}
		throw new IllegalStateException("spec not found: " + name);
	}

	private ToolExecutionRequest orderRequest(String arguments) {
		return ToolExecutionRequest.builder().id("t1").name("order").arguments(arguments).build();
	}

	/** 两个必填参数只传第一个，构造缺参请求（参数名从 spec 取，与编译参数名无关） */
	private String partialOrderArguments() {
		List<String> required = spec("order").parameters().required();
		assertThat(required).hasSize(2);
		return "{\"" + required.get(0) + "\":\"p1\"}";
	}

	@Test
	void missingRequiredArgumentIsInterceptedBeforeExecutionAndDefaultHandlerAsksUser() {
		GuardedToolExecutor executor = new GuardedToolExecutor(spec("order"), tool,
				ToolArgumentsErrorHandler.DEFAULT);

		String text = executor.execute(orderRequest(partialOrderArguments()), "m1");

		assertThat(text).contains("缺少必要参数").contains("向用户询问");
		assertThat(text).contains(spec("order").parameters().required().get(1));
		assertThat(tool.invoked).isFalse();
	}

	@Test
	void missingArgumentWithCustomHandlerReturnsBusinessText() {
		GuardedToolExecutor executor = new GuardedToolExecutor(spec("order"), tool,
				(error, request) -> "业务定制提示：请用户补齐");

		String text = executor.execute(orderRequest(partialOrderArguments()), "m1");

		assertThat(text).isEqualTo("业务定制提示：请用户补齐");
		assertThat(tool.invoked).isFalse();
	}

	@Test
	void invalidArgumentTypeReturnsDefaultRetryTextInsteadOfThrowing() {
		List<String> required = spec("order").parameters().required();
		String arguments = "{\"" + required.get(0) + "\":\"p1\",\"" + required.get(1) + "\":\"abc\"}";
		GuardedToolExecutor executor = new GuardedToolExecutor(spec("order"), tool,
				ToolArgumentsErrorHandler.DEFAULT);

		String text = executor.execute(orderRequest(arguments), "m1");

		assertThat(text).contains("格式错误").contains("修正");
		assertThat(tool.invoked).isFalse();
	}

	@Test
	void validArgumentsPassThroughAndOptionalParameterMayBeAbsent() {
		List<String> required = spec("order").parameters().required();
		String arguments = "{\"" + required.get(0) + "\":\"p1\",\"" + required.get(1) + "\":\"2\"}";
		GuardedToolExecutor executor = new GuardedToolExecutor(spec("order"), tool,
				ToolArgumentsErrorHandler.DEFAULT);

		String text = executor.execute(orderRequest(arguments), "m1");

		// optional 的 remark 未传不拦截；正常结果透传
		assertThat(text).isEqualTo("OK:p1:2");
		assertThat(tool.invoked).isTrue();
	}

	@Test
	void blankArgumentsWithRequiredParametersFailsAsMissing() {
		GuardedToolExecutor executor = new GuardedToolExecutor(spec("order"), tool,
				ToolArgumentsErrorHandler.DEFAULT);
		ToolExecutionRequest request = ToolExecutionRequest.builder()
				.id("t1").name("order").arguments(null).build();

		String text = executor.execute(request, "m1");

		assertThat(text).contains("缺少必要参数");
		assertThat(tool.invoked).isFalse();
	}

	@Test
	void toolWithoutRequiredParametersExecutesNormally() {
		GuardedToolExecutor executor = new GuardedToolExecutor(spec("ping"), tool,
				ToolArgumentsErrorHandler.DEFAULT);
		ToolExecutionRequest request = ToolExecutionRequest.builder()
				.id("t2").name("ping").arguments("{}").build();

		String text = executor.execute(request, "m1");

		assertThat(text).isEqualTo("pong");
		assertThat(tool.invoked).isTrue();
	}

	@Test
	void handlerReThrowPropagatesToCaller() {
		GuardedToolExecutor executor = new GuardedToolExecutor(spec("order"), tool, (error, request) -> {
			throw new IllegalStateException("业务决定中断");
		});

		assertThatThrownBy(() -> executor.execute(orderRequest(partialOrderArguments()), "m1"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("业务决定中断");
		assertThat(tool.invoked).isFalse();
	}

	private ToolExecutionRequest emptyRequest(String name) {
		return ToolExecutionRequest.builder().id("t-" + name).name(name).arguments("{}").build();
	}

	@Test
	void llmVisibleExceptionMessageIsReturnedToLlm() {
		GuardedToolExecutor executor = new GuardedToolExecutor(spec("risk"), tool,
				ToolArgumentsErrorHandler.DEFAULT);

		String text = executor.execute(emptyRequest("risk"), "m1");

		// 0.35 DefaultToolExecutor 捕获后取 cause.getMessage() → 消息回给 LLM
		assertThat(text).isEqualTo("库存不足，请让用户减少购买数量");
		assertThat(tool.invoked).isTrue();
	}

	@Test
	void nullMessageExceptionYieldsNonNullPlaceholder() {
		GuardedToolExecutor executor = new GuardedToolExecutor(spec("silentFail"), tool,
				ToolArgumentsErrorHandler.DEFAULT);

		String text = executor.execute(emptyRequest("silentFail"), "m1");

		// 0.35 下无消息异常返回 null → 出口兜底为非空占位文本
		assertThat(text).isNotNull();
		assertThat(text).contains("silentFail").contains("空结果");
		assertThat(tool.invoked).isTrue();
	}

	@Test
	void llmVisibleExceptionFromDelegateBypassesHandlerAndReturnsMessage() {
		ToolExecutor throwingDelegate = (request, memoryId) -> {
			throw LlmVisibleToolExecutionException.from("直接抛出的可见消息");
		};
		GuardedToolExecutor executor = new GuardedToolExecutor(spec("ping"), throwingDelegate,
				(error, request) -> "HANDLER文本");

		String text = executor.execute(emptyRequest("ping"), "m1");

		assertThat(text).isEqualTo("直接抛出的可见消息");
	}

	@Test
	void llmVisibleFromRejectsNullOrBlankMessage() {
		assertThatThrownBy(() -> LlmVisibleToolExecutionException.from(null))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> LlmVisibleToolExecutionException.from("   "))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
