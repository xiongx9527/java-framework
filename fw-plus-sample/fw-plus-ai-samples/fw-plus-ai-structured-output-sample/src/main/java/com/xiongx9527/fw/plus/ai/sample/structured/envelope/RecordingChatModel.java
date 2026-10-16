package com.xiongx9527.fw.plus.ai.sample.structured.envelope;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.BusinessRegistry;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 离线假模型：<b>一个类同时承担两种请求</b>，样例因此不需要任何模型服务、任何 API Key。
 *
 * <p>它按提示词内容判断这一轮要什么：</p>
 *
 * <ol>
 *   <li>提示词里带分类器的固定开头（{@link #CLASSIFIER_MARKER}）→ 返回业务分类结果
 *       （真实场景由模型理解语义，这里按关键词判，够演示整条链路）；</li>
 *   <li>否则按关键词返回业务结果或闲聊正文。</li>
 * </ol>
 *
 * <p>它把每一轮请求都记下来：测试用 {@link #requestCount()} 分辨「这一轮发了几次模型请求」，
 * 因此可以断言「分类发生了」「重试发生了」；用 {@link #lastRequestText()} 看上下文里到底带了什么，
 * 因此可以断言「历史被带进了下一轮」。</p>
 */
public final class RecordingChatModel implements ChatModel {

	/** 分类请求的识别标记：与 {@code BusinessClassifier} 生成的提示词开头一致 */
	public static final String CLASSIFIER_MARKER = "请判断下面这轮用户消息属于哪个业务";

	/** 未命中任何业务时给出的闲聊正文（同时是期望模型原文） */
	public static final String CHAT_REPLY = "你好呀，我是结构化输出样例里的假模型；这轮没命中业务，按闲聊回你。";

	private final List<List<ChatMessage>> requests = new ArrayList<List<ChatMessage>>();

	/** 全部请求（按发生顺序；元素是不可变的副本） */
	public List<List<ChatMessage>> requests() {
		List<List<ChatMessage>> copies = new ArrayList<List<ChatMessage>>();
		for (List<ChatMessage> request : requests) {
			copies.add(Collections.unmodifiableList(request));
		}
		return Collections.unmodifiableList(copies);
	}

	/** 至今发过几次模型请求（分类、结构化生成、闲聊都算） */
	public int requestCount() {
		return requests.size();
	}

	/** 最近一次请求的全部文本（系统提示 + 历史 + 本轮提示词拼在一起） */
	public String lastRequestText() {
		if (requests.isEmpty()) {
			return "";
		}
		return fullText(requests.get(requests.size() - 1));
	}

	@Override
	public ChatResponse doChat(ChatRequest request) {
		List<ChatMessage> messages = request.messages();
		requests.add(new ArrayList<ChatMessage>(messages));
		return ChatResponse.builder()
				.aiMessage(AiMessage.from(reply(fullText(messages))))
				.build();
	}

	// -------------------------------------------------- 假模型的“理解”

	/** 模型看到的是全部消息：系统提示 + 历史 + 本轮提示词，因此按整段文本判定 */
	private static String reply(String text) {
		if (text.contains(CLASSIFIER_MARKER)) {
			return "{\"business\":\"" + classify(text) + "\"}";
		}
		if (text.contains("退款")) {
			// 说不清订单号与理由 → 反问；说清了 → 直接给出结果。注意这两次都是 ok=true
			return text.contains("订单号")
					? "{\"needMoreInfo\":false,\"orderId\":\"A1001\",\"reason\":\"收货时外箱破损\"}"
					: "{\"needMoreInfo\":true,\"question\":\"请告诉我订单号与退款原因\"}";
		}
		if (text.contains("订单")) {
			// BAD 单号：模型声明不需要补充信息却没给订单号 → 契约判失败（演示重试与失败信封）
			return text.contains("BAD")
					? "{\"needMoreInfo\":false}"
					: "{\"needMoreInfo\":false,\"orderId\":\"A1001\",\"status\":\"paid\"}";
		}
		return CHAT_REPLY;
	}

	/**
	 * 业务分类：命中已注册业务名或兜底；历史也在这段文本里，所以上一轮说了什么会影响本轮判定。
	 *
	 * <p>分类器的提示词只列<b>已注册的业务名</b>，没有「都不匹配」这个出口：真实模型遇到都不匹配时
	 * 通常会给一个枚举外的取值，框架随即归入兜底 {@code chat}（这是规格允许的回落路径）。
	 * 这里按关键词给值、判不出来时直接回兜底名，等价于模型给枚举外取值的效果。</p>
	 */
	private static String classify(String text) {
		if (text.contains("退款")) {
			return SampleBusinesses.REFUND;
		}
		if (text.contains("订单")) {
			return SampleBusinesses.ORDER;
		}
		return BusinessRegistry.FALLBACK;
	}

	private static String fullText(List<ChatMessage> messages) {
		StringBuilder text = new StringBuilder();
		for (ChatMessage message : messages) {
			text.append(textOf(message)).append('\n');
		}
		return text.toString();
	}

	private static String textOf(ChatMessage message) {
		if (message instanceof UserMessage) {
			return ((UserMessage) message).singleText();
		}
		if (message instanceof SystemMessage) {
			return ((SystemMessage) message).text();
		}
		if (message instanceof AiMessage) {
			return ((AiMessage) message).text();
		}
		return "";
	}
}
