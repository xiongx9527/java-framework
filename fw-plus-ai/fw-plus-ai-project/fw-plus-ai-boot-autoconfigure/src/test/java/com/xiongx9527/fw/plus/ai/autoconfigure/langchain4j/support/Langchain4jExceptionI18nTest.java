package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.LlmVisibleToolExecutionException;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.MissingArgumentException;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutputException;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;

/**
 * AI 层框架异常的国际化文案测试。
 *
 * <p>验证迁移后的异常以「具体错误码 + 参数」构造时，能按指定语言解析出中文 / 英文文案；
 * 也是资源文件 key 缺失或占位符错位的兜底回归。</p>
 */
@DisplayName("AI 层异常国际化文案")
class Langchain4jExceptionI18nTest {

	private static final Locale ZH = Locale.SIMPLIFIED_CHINESE;
	private static final Locale EN = Locale.US;

	@Test
	@DisplayName("工具可见异常：显式消息原样透传")
	void llmVisibleToolExecutionException_keepsExplicitMessage() {
		LlmVisibleToolExecutionException exception = LlmVisibleToolExecutionException.from("用户不存在");
		assertEquals(50_005_001, exception.getCode());
		assertEquals("用户不存在", exception.getMessage(ZH));
		assertEquals("用户不存在", exception.getMessage(EN));
	}

	@Test
	@DisplayName("必填参数缺失：中文（默认）")
	void missingArgumentException_chinese() {
		MissingArgumentException exception = new MissingArgumentException("queryUser", Arrays.asList("userId"));
		assertEquals(40_001_001, exception.getCode());
		assertEquals("工具 'queryUser' 缺少必填参数：[userId]", exception.getMessage(ZH));
	}

	@Test
	@DisplayName("必填参数缺失：英文")
	void missingArgumentException_english() {
		MissingArgumentException exception = new MissingArgumentException("queryUser", Arrays.asList("userId"));
		assertEquals("Tool 'queryUser' is missing required argument(s): [userId]", exception.getMessage(EN));
	}

	@Test
	@DisplayName("结构化输出：返回为空")
	void structuredOutputException_replyEmpty() {
		StructuredOutputException exception =
				StructuredOutputException.of(StructuredOutputException.REPLY_EMPTY, null, null);
		assertEquals(50_024_101, exception.getCode());
		assertEquals("模型返回为空，无法解析为 JSON", exception.getMessage(ZH));
		assertEquals("Model returned an empty reply; cannot parse as JSON", exception.getMessage(EN));
	}

	@Test
	@DisplayName("结构化输出：非法 JSON 带一个占位参数")
	void structuredOutputException_invalidJson() {
		StructuredOutputException exception = StructuredOutputException.of(
				StructuredOutputException.INVALID_JSON, "{oops", null, "syntax error");
		assertEquals("返回内容不是合法 JSON：syntax error", exception.getMessage(ZH));
		assertEquals("Reply is not valid JSON: syntax error", exception.getMessage(EN));
	}

	@Test
	@DisplayName("结构化输出：尝试次数用尽，携带原文与错误清单")
	void structuredOutputException_attemptsExhausted() {
		StructuredOutputException exception = StructuredOutputException.of(
				StructuredOutputException.ATTEMPTS_EXHAUSTED, "bad reply", Arrays.asList("缺少 orderId"), null,
				3, "缺少 orderId");
		assertEquals(50_024_105, exception.getCode());
		assertEquals("结构化输出失败（已尝试 3 次）：缺少 orderId", exception.getMessage(ZH));
		assertEquals("Structured output failed (already tried 3 time(s)): 缺少 orderId", exception.getMessage(EN));
		assertEquals("bad reply", exception.getLastReply());
		assertEquals(Arrays.asList("缺少 orderId"), exception.getErrors());
	}

	@Test
	@DisplayName("具名 key：脚本不支持设置模型实例")
	void namedKey_modelModuleScriptSetUnsupported() {
		Locale previous = LocaleContextHolder.getLocale();
		try {
			LocaleContextHolder.setLocale(ZH);
			assertEquals("脚本不支持设置模型实例，请在控制台维护 对话模型 数据源",
					I18nMessageResolver.resolveKey("fw.ai.modelModule.scriptSetUnsupported", "对话模型"));
			LocaleContextHolder.setLocale(EN);
			assertEquals("Setting a model instance from scripts is not supported; manage the Chat model datasource in the console",
					I18nMessageResolver.resolveKey("fw.ai.modelModule.scriptSetUnsupported", "Chat model"));
		}
		finally {
			LocaleContextHolder.setLocale(previous);
		}
	}

	@Test
	@DisplayName("未定义资源的状态码解析为 null，不抛异常")
	void undefinedResourceResolvesToNull() {
		assertNull(I18nMessageResolver.resolve(50_024_099, ZH));
	}
}
