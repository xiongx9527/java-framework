package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

/**
 * 大模型补全抽象：把「系统提示 + 用户文本」交给一个文本补全实现，返回模型原始文本。
 *
 * <p>之所以不直接依赖具体的大模型接口，是为了与 LangChain4j 的模型抽象解耦
 * （不同版本接口名不同：{@code ChatLanguageModel} / {@code ChatModel}）。使用方用
 * lambda 适配自己的模型即可，例如：</p>
 *
 * <pre>{@code
 * IntentLlm llm = (system, user) -> chatModel
 *         .generate(SystemMessage.from(system), UserMessage.from(user))
 *         .content().text();
 * }</pre>
 */
@FunctionalInterface
public interface IntentLlm {

	/**
	 * 依据提示词返回模型原始文本（约定为 JSON，由 {@link LlmIntentClassifier} 解析）。
	 *
	 * @param systemPrompt 系统提示（含意图清单与输出格式约束）
	 * @param userPrompt   用户输入文本
	 */
	String complete(String systemPrompt, String userPrompt);
}
