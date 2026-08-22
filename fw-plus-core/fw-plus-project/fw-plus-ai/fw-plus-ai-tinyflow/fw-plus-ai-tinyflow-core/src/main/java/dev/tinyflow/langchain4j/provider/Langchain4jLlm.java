package dev.tinyflow.langchain4j.provider;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.llm.Llm;
import dev.tinyflow.core.node.LlmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Langchain4jLlm implements Llm {

	private final ChatModel chatModel;

	public Langchain4jLlm(ChatModel chatModel) {
		this.chatModel = Objects.requireNonNull(chatModel, "chatModel");
	}

	@Override
	public String chat(MessageInfo message, ChatOptions options, LlmNode node, Chain chain) {
		List<ChatMessage> messages;
		try {
			messages = toMessages(message);
		} catch (RuntimeException e) {
			throw new TinyflowException("Failed to map Tinyflow message to LangChain4j");
		}

		ChatResponse response;
		try {
			response = chatModel.chat(messages);
		} catch (RuntimeException e) {
			throw new TinyflowException("LangChain4j model invocation failed", e);
		}
		if (response == null || response.aiMessage() == null || isBlank(response.aiMessage().text())) {
			throw new TinyflowException("LangChain4j model returned an empty response");
		}
		return response.aiMessage().text();
	}

	private List<ChatMessage> toMessages(MessageInfo message) {
		List<ChatMessage> messages = new ArrayList<ChatMessage>();
		if (!isBlank(message.getSystemMessage())) {
			messages.add(SystemMessage.from(message.getSystemMessage()));
		}

		List<Content> contents = new ArrayList<Content>();
		contents.add(TextContent.from(message.getMessage()));
		if (message.getImages() != null) {
			for (String image : message.getImages()) {
				contents.add(ImageContent.from(image));
			}
		}
		messages.add(UserMessage.from(contents));
		return messages;
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
