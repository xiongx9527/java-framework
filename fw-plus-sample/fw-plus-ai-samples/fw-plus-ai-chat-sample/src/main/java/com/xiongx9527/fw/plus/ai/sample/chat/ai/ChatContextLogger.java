package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 模型调用上下文日志：把每次发给模型的内容按类型拆开打印。
 *
 * <p>覆盖六类内容，对应排查 prompt 拼接效果时最常看的几处：</p>
 * <ul>
 *   <li><b>TOOLS</b>：本轮可用工具清单（{@code ToolSpecification}），即模型能看到的工具能力边界；</li>
 *   <li><b>SYSTEM</b>：系统提示词，由 {@code ChatAssistantConfig.systemMessageProvider} 生成，
 *       <b>末尾的「可参考的资料」就是 RAG 检索结果</b>（经 {@code @V("ragContext")} 注入）；</li>
 *   <li><b>USER</b>：用户原话（检索内容不在这里）；</li>
 *   <li><b>TOOL_CALL</b>：模型请求执行的工具及参数（{@code AiMessage.toolExecutionRequests}）；</li>
 *   <li><b>TOOL_RESPONSE</b>：工具执行结果（{@code ToolExecutionResultMessage}）；</li>
 *   <li><b>RESPONSE</b>：模型回复与 token 用量。</li>
 * </ul>
 *
 * <p>工具参数的完整 JSON schema 不在这里打印（每个工具都很长），需要时打开
 * {@code langchain4j.ollama.chat-model.log-requests} 看原始请求体。</p>
 *
 * <p>该 Bean 由框架侧 provider 自动配置统一挂载（{@code ObjectProvider<List<ChatModelListener>>}），
 * 样例无需自己找模型 Bean。同步与流式两条链路都会经过这里。</p>
 */
@Component
public class ChatContextLogger implements ChatModelListener {

	/** 单条消息最长打印字符数，超出截断，避免知识库长文与长回复刷屏。 */
	static final int MAX_LENGTH = 600;

	private static final Logger log = LoggerFactory.getLogger(ChatContextLogger.class);

	@Override
	public void onRequest(ChatModelRequestContext requestContext) {
		List<ChatMessage> messages = requestContext.request().messages();
		log.info("════════ 模型请求上下文（{} 条消息） ════════", messages.size());
		logTools(requestContext.request().toolSpecifications());
		int index = 0;
		for (ChatMessage message : messages) {
			logMessage(index++, message);
		}
	}

	private void logTools(List<ToolSpecification> toolSpecifications) {
		if (toolSpecifications == null || toolSpecifications.isEmpty()) {
			log.info("TOOLS        本轮未注册工具");
			return;
		}
		log.info("TOOLS        可用工具 {} 个", toolSpecifications.size());
		for (ToolSpecification tool : toolSpecifications) {
			log.info("TOOLS        - {} : {}", tool.name(), abbreviate(tool.description()));
		}
	}

	@Override
	public void onResponse(ChatModelResponseContext responseContext) {
		AiMessage aiMessage = responseContext.response().aiMessage();
		if (aiMessage.hasToolExecutionRequests()) {
			for (ToolExecutionRequest request : aiMessage.toolExecutionRequests()) {
				log.info("TOOL_CALL    模型请求工具 {} 参数 {}", request.name(), request.arguments());
			}
		} else {
			log.info("RESPONSE     模型回复: {}", abbreviate(aiMessage.text()));
		}
		TokenUsage tokenUsage = responseContext.response().tokenUsage();
		if (tokenUsage != null) {
			log.info("RESPONSE     token 用量 {}", tokenUsage);
		}
	}

	@Override
	public void onError(ChatModelErrorContext errorContext) {
		log.warn("模型调用失败: {}", errorContext.error().toString());
	}

	private void logMessage(int index, ChatMessage message) {
		if (message.type() == ChatMessageType.SYSTEM) {
			log.info("[{}] SYSTEM      {}", index, abbreviate(((SystemMessage) message).text()));
		} else if (message.type() == ChatMessageType.USER) {
			logUserMessage(index, (UserMessage) message);
		} else if (message.type() == ChatMessageType.TOOL_EXECUTION_RESULT) {
			ToolExecutionResultMessage result = (ToolExecutionResultMessage) message;
			log.info("[{}] TOOL_RESPONSE 工具 {} 返回: {}",
					index, result.toolName(), abbreviate(result.text()));
		} else if (message.type() == ChatMessageType.AI) {
			logAiMessage(index, (AiMessage) message);
		}
	}

	private void logUserMessage(int index, UserMessage message) {
		int anchor = message.text().indexOf(RagContentInjector.SEPARATOR);
		if (anchor < 0) {
			log.info("[{}] USER        {}", index, abbreviate(message.text()));
			return;
		}
		// user message = 用户原话 + 分隔串 + 检索内容（后者由 retrievalAugmentor 拼入）
		log.info("[{}] USER        {}", index, abbreviate(message.text().substring(0, anchor)));
		log.info("[{}] RAG         注入 {} 字符: {}",
				index, message.text().length() - anchor - RagContentInjector.SEPARATOR.length(),
				abbreviate(message.text().substring(anchor + RagContentInjector.SEPARATOR.length())));
	}

	private void logAiMessage(int index, AiMessage message) {
		if (!message.hasToolExecutionRequests()) {
			log.info("[{}] AI          历史回复: {}", index, abbreviate(message.text()));
			return;
		}
		for (ToolExecutionRequest request : message.toolExecutionRequests()) {
			log.info("[{}] TOOL_CALL    历史工具请求 {} 参数 {}", index, request.name(), request.arguments());
		}
	}

	/** 归一化换行后截断，防止上下文日志淹没其他输出。 */
	static String abbreviate(String text) {
		if (text == null) {
			return "null";
		}
		String normalized = text.replace('\n', ' ').replace('\r', ' ').trim();
		if (normalized.length() <= MAX_LENGTH) {
			return normalized;
		}
		return normalized.substring(0, MAX_LENGTH) + " …[已截断，原长 " + text.length() + " 字符]";
	}
}
