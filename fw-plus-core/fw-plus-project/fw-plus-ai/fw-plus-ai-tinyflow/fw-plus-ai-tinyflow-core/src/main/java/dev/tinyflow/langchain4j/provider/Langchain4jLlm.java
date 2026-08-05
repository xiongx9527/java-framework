package dev.tinyflow.langchain4j.provider;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.tinyflow.core.chain.Chain;
import dev.tinyflow.core.llm.Llm;
import dev.tinyflow.core.node.LlmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Langchain4jLlm implements Llm {

	private final ChatLanguageModel chatLanguageModel;

	public Langchain4jLlm(ChatLanguageModel chatLanguageModel) {
		this.chatLanguageModel = Objects.requireNonNull(chatLanguageModel, "chatLanguageModel");
	}

	@Override
	public String chat(MessageInfo message, ChatOptions options, LlmNode node, Chain chain) {
		List<ChatMessage> messages;
		try {
			messages = toMessages(message);
		} catch (RuntimeException e) {
			throw new TinyflowException("Failed to map Tinyflow message to LangChain4j");
		}

		Response<AiMessage> response;
		try {
			response = chatLanguageModel.generate(messages);
		} catch (RuntimeException e) {
			throw new TinyflowException("LangChain4j model invocation failed", e);
		}
		if (response == null || response.content() == null || isBlank(response.content().text())) {
			throw new TinyflowException("LangChain4j model returned an empty response");
		}
		return response.content().text();
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
