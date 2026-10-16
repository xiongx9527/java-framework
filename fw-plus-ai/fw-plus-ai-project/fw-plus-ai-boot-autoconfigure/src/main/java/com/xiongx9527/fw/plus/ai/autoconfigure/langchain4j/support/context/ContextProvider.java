package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.context;

/**
 * 统一上下文来源：为「本轮请求」产出一段可选的文本块。返回 {@code null} 或空白表示本来源本轮无内容，
 * 由注入器跳过，不影响其它来源。
 */
public interface ContextProvider {

	/**
	 * 产出本轮上下文文本；null / 空白表示无内容。
	 */
	String provide(ContextRequest request);

}