package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * 结构化输出失败：模型返回无法解析为 JSON，或多次尝试后仍未通过校验。
 *
 * <p>{@link #getLastReply()} 保留最后一次原始回复（便于排查或人工兜底），
 * {@link #getErrors()} 是最后一次的校验错误清单（已回喂给模型）。</p>
 */
public class StructuredOutputException extends BizException {

	private static final long serialVersionUID = 1L;

	private final String lastReply;

	private final List<String> errors;

	public StructuredOutputException(String message) {
		this(message, null, null, null);
	}

	public StructuredOutputException(String message, Throwable cause) {
		this(message, null, null, cause);
	}

	public StructuredOutputException(String message, String lastReply, List<String> errors) {
		this(message, lastReply, errors, null);
	}

	public StructuredOutputException(String message, String lastReply, List<String> errors, Throwable cause) {
		super(message, cause);
		this.lastReply = lastReply;
		this.errors = errors == null ? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(errors));
	}

	/** 最后一次原始回复；解析前就失败（如模型调用异常）时为 {@code null} */
	public String getLastReply() {
		return lastReply;
	}

	/** 最后一次校验错误清单（未校验过时为空列表） */
	public List<String> getErrors() {
		return errors;
	}
}
