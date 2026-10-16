package com.xiongx9527.fw.plus.ai.sample.structured.envelope;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 离线假流式模型：逐字回调（这是 {@code token} 帧<b>唯一</b>的来源），并记录自己被调用了几次、发了哪些字。
 *
 * <p>样例用它验证帧协议里最要紧的一条：<b>结构化轮次不发 {@code token} 帧</b>。
 * 结构化轮次由 {@code EnvelopeRuntime} 直连<b>非流式</b>模型一次性生成，所以本类一次都不会被调用；
 * 反过来，闲聊轮次会逐字下发，本类必定被调用。</p>
 */
public final class RecordingStreamingChatModel implements StreamingChatModel {

	/**
	 * 逐字下发的分片：拼起来正好等于闲聊假模型给的正文。
	 *
	 * <p>真实场景两条通道本来就是同一个模型，所以这里也保持一致——这样「同形」断言能一路比到
	 * {@code text}（否则文本不同只是两个假模型不一致，不是框架的事）。</p>
	 */
	private static final String[] TOKENS = split(RecordingChatModel.CHAT_REPLY);

	private static String[] split(String reply) {
		int middle = reply.length() / 2;
		return new String[] {reply.substring(0, middle), reply.substring(middle)};
	}

	private final List<String> requests = new ArrayList<String>();

	private final List<String> deliveredTokens = new ArrayList<String>();

	/** 至今被调用几次（每次调用都会产生一串 token 帧） */
	public int requestCount() {
		return requests.size();
	}

	/** 实际回调出去的 token 分片（按顺序）；为空即「没有任何 token 帧来源」 */
	public List<String> deliveredTokens() {
		return Collections.unmodifiableList(deliveredTokens);
	}

	@Override
	public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
		requests.add(join(request.messages()));
		StringBuilder full = new StringBuilder();
		for (String token : TOKENS) {
			deliveredTokens.add(token);
			handler.onPartialResponse(token);
			full.append(token);
		}
		handler.onCompleteResponse(ChatResponse.builder()
				.aiMessage(AiMessage.from(full.toString()))
				.build());
	}

	private static String join(List<ChatMessage> messages) {
		StringBuilder text = new StringBuilder();
		for (ChatMessage message : messages) {
			text.append(message).append('\n');
		}
		return text.toString();
	}
}
