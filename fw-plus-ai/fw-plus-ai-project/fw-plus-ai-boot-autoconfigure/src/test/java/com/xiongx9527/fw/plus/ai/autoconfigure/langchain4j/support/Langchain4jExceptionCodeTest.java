package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.LlmVisibleToolExecutionException;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.MissingArgumentException;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutputException;

/**
 * 锁定 AI 层框架异常的固定状态码。
 */
class Langchain4jExceptionCodeTest {

	@Test
	void missingArgumentExceptionCarriesRequestCode() {
		assertEquals(40_001_001, MissingArgumentException.CODE);

		MissingArgumentException exception = new MissingArgumentException("queryUser", Collections.singletonList("userId"));
		assertEquals(40_001_001, exception.getCode());
		assertEquals("工具 'queryUser' 缺少必填参数：[userId]", exception.getMessage());
	}

	@Test
	void llmVisibleToolExecutionExceptionCarriesBusinessCode() {
		assertEquals(50_005_001, LlmVisibleToolExecutionException.CODE);

		LlmVisibleToolExecutionException exception = LlmVisibleToolExecutionException.from("用户不存在");
		assertEquals(50_005_001, exception.getCode());
		assertEquals("用户不存在", exception.getMessage());
	}

	@Test
	void structuredOutputExceptionCarriesSerializationCode() {
		assertEquals(50_024_001, StructuredOutputException.CODE);
		assertEquals(50_024_001, new StructuredOutputException("解析失败").getCode());
		assertEquals(50_024_001,
				new StructuredOutputException("解析失败", "{\"a\":1}", Collections.singletonList("必须为对象")).getCode());
	}
}
