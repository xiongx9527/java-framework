package com.xiongx9527.fw.plus.ai.askdata.api;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AskData Core 公共 API 的最小契约测试。
 */
class AskDataApiContractTest {

	@Test
	void 应拒绝空白问题() {
		AskDataProjectContext context = new AskDataProjectContext("project-1", "operator-1");

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> new AskDataRequest(context, " \t\n "));

		assertTrue(exception.getMessage().contains("question"));
	}

	@Test
	void 应要求非空白项目和操作人标识() {
		assertThrows(IllegalArgumentException.class,
				() -> new AskDataProjectContext(" ", "operator-1"));
		assertThrows(IllegalArgumentException.class,
				() -> new AskDataProjectContext("project-1", "\t"));
	}

	@Test
	void 应拒绝Unicode空白问题和隔离标识() {
		AskDataProjectContext context = new AskDataProjectContext("project-1", "operator-1");

		assertThrows(IllegalArgumentException.class,
				() -> new AskDataRequest(context, "\u2003"));
		assertThrows(IllegalArgumentException.class,
				() -> new AskDataProjectContext("\u2003", "operator-1"));
		assertThrows(IllegalArgumentException.class,
				() -> new AskDataProjectContext("project-1", "\u2003"));
	}

	@Test
	void 应按Unicode空白规则规范化请求与隔离标识() {
		AskDataProjectContext context = new AskDataProjectContext("\u2003project-1\u2003", "\u2003operator-1\u2003");
		AskDataRequest request = new AskDataRequest(context, "\u2003查询项目数据\u2003");

		assertEquals("project-1", context.getProjectId());
		assertEquals("operator-1", context.getOperatorId());
		assertEquals("查询项目数据", request.getQuestion());
	}

	@Test
	void 应拒绝Unicode空白错误码和说明() {
		assertThrows(IllegalArgumentException.class,
				() -> new AskDataError("\u2003", "查询失败"));
		assertThrows(IllegalArgumentException.class,
				() -> new AskDataError("ASKDATA_FAILED", "\u2003"));
	}

	@Test
	void 应按Unicode空白规则规范化错误码和说明() {
		AskDataError error = new AskDataError("\u2003ASKDATA_FAILED\u2003", "\u2003查询失败\u2003");

		assertEquals("ASKDATA_FAILED", error.getCode());
		assertEquals("查询失败", error.getMessage());
	}

	@Test
	void 公共契约不得绑定具体AI或Spring框架包() {
		assertNotFrameworkType(AskDataService.class);
		assertNotFrameworkType(AskDataRequest.class);
		assertNotFrameworkType(AskDataResult.class);
		assertNotFrameworkType(AskDataError.class);
		assertNotFrameworkType(AskDataProjectContext.class);
	}

	@Test
	void 服务应暴露统一请求与结果契约() throws NoSuchMethodException {
		Method method = AskDataService.class.getMethod("ask", AskDataRequest.class);

		assertEquals(AskDataResult.class, method.getReturnType());
	}

	@Test
	void 结果应通过错误对象表达失败状态() {
		AskDataResult success = AskDataResult.success("查询结果");
		AskDataError error = new AskDataError("ASKDATA_FAILED", "查询失败");
		AskDataResult failure = AskDataResult.failure(error);

		assertTrue(success.isSuccess());
		assertEquals("查询结果", success.getAnswer());
		assertTrue(!failure.isSuccess());
		assertEquals(error, failure.getError());
	}

	private void assertNotFrameworkType(Class<?> type) {
		String className = type.getName();
		assertTrue(!className.startsWith("org.springframework"));
		assertTrue(!className.startsWith("dev.langchain4j"));
		assertTrue(!className.startsWith("io.agentscope"));
		assertTrue(!className.startsWith("org.springframework.ai"));
	}
}
