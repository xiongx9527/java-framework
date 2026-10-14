package com.xiongx9527.fw.plus.ai.sample.structured;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.AssistantBuilder;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant.SimpleAssistant;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.Rule;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutput;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutputException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 结构化输出样例：让大模型稳定吐出一个可用的 JSON 对象。
 *
 * <p>三步：<b>准备 schema</b>（一份两用：生成提示词指令 + 应用侧校验）→ <b>拿到 assistant</b> →
 * <b>{@code chat(...)} 一条龙，或者只用零件自己写循环</b>。</p>
 *
 * <p>{@link #main(String[])} <b>不需要任何模型、任何 API Key</b>：它用一个假的
 * {@link StructuredOutput.ChatFunction}（预设"第一轮答歪、第二轮答对"）把
 * 指令 → 调用 → 解析 → 校验 → 错误回喂 的全过程打印出来，照着抄即可。</p>
 *
 * <p>接真实模型看 {@link #extractWith(ChatLanguageModel, String, String)}：
 * 任何 langchain4j {@code ChatLanguageModel}（openai / qwen / zhipu / ollama ...）都能装进
 * {@link SimpleAssistant}，本工具类对 provider 无要求。</p>
 */
public final class StructuredOutputSample {

	/** schema 放 resources 里，省掉长字符串转义 */
	private static final String SCHEMA_LOCATION = "ai/order-schema.json";

	private static final String USER_TEXT = "把订单信息抽出来：客户说已经付了 3 件，单号 A1001";

	private StructuredOutputSample() {
	}

	public static void main(String[] args) throws IOException {
		demoRetryWithFeedback();
		demoRetryExhausted();
	}

	// -------------------------------------------------- 演示 1：模型先答歪、再答对

	private static void demoRetryWithFeedback() throws IOException {
		StructuredOutput structuredOutput = orderExtractor();
		System.out.println("=== 演示 1：模型先答歪、再答对（错误自动回喂） ===");
		System.out.println("工具按 schema 生成的提示词指令：");
		System.out.println(structuredOutput.instruction());

		// 假模型：第 1 轮故意漏 orderId、类型写错、枚举越界；第 2 轮才答对 —— 专门用来演示错误回喂
		final List<String> prompts = new ArrayList<String>();
		final List<String> replies = new ArrayList<String>();
		StructuredOutput.ChatFunction fakeModel = new StructuredOutput.ChatFunction() {

			@Override
			public String send(String prompt) {
				prompts.add(prompt);
				String reply = prompts.size() == 1
						? "```json\n{\"quantity\":\"三件\",\"status\":\"done\"}\n```"
						: "{\"orderId\":\"A1001\",\"quantity\":3,\"status\":\"paid\"}";
				replies.add(reply);
				return reply;
			}
		};

		JSONObject order = structuredOutput.chat(fakeModel, USER_TEXT, 3);

		System.out.println("第 1 轮模型返回（包了 Markdown 代码块、字段还不合法）：" + replies.get(0));
		System.out.println("第 1 轮校验不通过，错误清单：" + validate(structuredOutput, replies.get(0)));
		System.out.println("       把上面这些错误回喂给模型，第 2 轮返回：" + replies.get(1));
		System.out.println("共发出 " + prompts.size() + " 次请求，最终拿到：" + order);
		System.out.println("取字段：orderId=" + order.getString("orderId")
				+ "，quantity=" + order.getIntValue("quantity")
				+ "，status=" + order.getString("status"));
		System.out.println();
	}

	// -------------------------------------------------- 演示 2：模型始终不给 JSON

	private static void demoRetryExhausted() throws IOException {
		StructuredOutput structuredOutput = orderExtractor();
		System.out.println("=== 演示 2：模型始终不吐 JSON（重试耗尽，抛出带上下文的异常） ===");

		StructuredOutput.ChatFunction alwaysBad = new StructuredOutput.ChatFunction() {

			@Override
			public String send(String prompt) {
				return "抱歉，我无法回答这个问题。";
			}
		};

		try {
			structuredOutput.chat(alwaysBad, USER_TEXT, 2);
			System.out.println("（不该走到这里）");
		} catch (StructuredOutputException e) {
			System.out.println("按预期抛出：" + e.getMessage());
			System.out.println("最后一次原文 lastReply：" + e.getLastReply());
			System.out.println("最后一次错误 errors：" + e.getErrors());
		}
	}

	// -------------------------------------------------- 接线：schema 与真实模型

	/**
	 * 一份 schema 两用：既生成提示词指令，又在应用侧校验返回值。
	 *
	 * <p>校验分两道 —— JSON Schema 管结构（只报第一个错），{@link Rule} 补齐全量错误清单与业务语义。</p>
	 */
	public static StructuredOutput orderExtractor() throws IOException {
		return StructuredOutput.of(loadSchema())
				// 必填 / 枚举 / 区间（内置工厂只作用于顶层字段）
				.rules(Rule.required("orderId"),
						Rule.enumOf("status", "paid", "unpaid"),
						Rule.range("quantity", 1, 99),
						// 业务语义（schema 表达不了的，直接写 lambda：返回 null 表示通过）
						value -> "paid".equals(value.getString("status")) && value.getIntValue("quantity") <= 0
								? "status=paid 时 quantity 必须大于 0" : null);
	}

	/**
	 * 接真实模型：任何 {@link ChatLanguageModel} 装进 {@link SimpleAssistant}，一行 {@code chat(...)} 搞定。
	 *
	 * <p>{@code memoryId} 决定会话记忆；重试复用同一个 id，模型因此能看到自己上一轮的错误输出。</p>
	 *
	 * <pre>{@code
	 * ChatLanguageModel model = OpenAiChatModel.builder()
	 *         .apiKey(System.getenv("OPENAI_API_KEY"))
	 *         .modelName("gpt-4o-mini")
	 *         .temperature(0.0)      // 结构化抽取：温度调 0 效果立竿见影
	 *         .build();
	 * JSONObject order = StructuredOutputSample.extractWith(model, "extract-A1001", text);
	 * }</pre>
	 */
	public static JSONObject extractWith(ChatLanguageModel chatModel, String memoryId, String text) throws IOException {
		SimpleAssistant assistant = new AssistantBuilder()
				.chatModel(chatModel)
				.memory(20)                 // 可选：会话记忆窗口
				.build();
		return orderExtractor().chat(assistant, memoryId, text);
	}

	/** 零件模式：指令自己拼、回复自己解析、错误自己回喂（把循环攥在自己手里时用） */
	public static JSONObject extractManually(SimpleAssistant assistant, String memoryId, String text) throws IOException {
		StructuredOutput structuredOutput = orderExtractor();
		String reply = assistant.chat(memoryId, structuredOutput.prompt(text));
		JSONObject value = structuredOutput.parse(reply);
		StructuredOutput.Validation validation = structuredOutput.validate(value);
		if (!validation.isValid()) {
			// 自己决定怎么处理：回喂重试、记日志、走人工兜底……
			throw new StructuredOutputException("校验不通过：" + validation.message(), reply, validation.errors());
		}
		return value;
	}

	private static List<String> validate(StructuredOutput structuredOutput, String reply) {
		return structuredOutput.validate(structuredOutput.parse(reply)).errors();
	}

	private static String loadSchema() throws IOException {
		InputStream inputStream = new ClassPathResource(SCHEMA_LOCATION).getInputStream();
		try {
			return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
		} finally {
			inputStream.close();
		}
	}
}
