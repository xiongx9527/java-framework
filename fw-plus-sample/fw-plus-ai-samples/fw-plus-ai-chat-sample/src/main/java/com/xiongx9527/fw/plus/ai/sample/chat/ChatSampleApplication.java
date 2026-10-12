package com.xiongx9527.fw.plus.ai.sample.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * fw-plus 大模型对话样例。
 *
 * <p>演示：Ollama 本地模型 + 远程知识库接口（RAG）+ 天气工具调用 + SSE 流式。</p>
 */
@SpringBootApplication
public class ChatSampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatSampleApplication.class, args);
	}
}
