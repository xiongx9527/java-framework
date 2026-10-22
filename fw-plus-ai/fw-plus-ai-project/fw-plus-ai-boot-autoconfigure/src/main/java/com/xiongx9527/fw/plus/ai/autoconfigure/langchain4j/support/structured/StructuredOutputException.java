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
 *
 * <p>文案由国际化资源文件按状态码提供，见 {@code i18n/messages.properties}
 * 中 {@code 50024101} - {@code 50024105} 段。</p>
 */
public class StructuredOutputException extends BizException {

	private static final long serialVersionUID = 1L;

	/** 状态码 50024001（SERVER/SERIALIZATION/001 模型结构化输出解析失败，类通用兜底码） */
	public static final int CODE = 50_024_001;

	/** 模型返回为空，无法解析为 JSON */
	public static final int REPLY_EMPTY = 50_024_101;
	/** 返回内容顶层是数组 */
	public static final int TOP_LEVEL_ARRAY = 50_024_102;
	/** 返回内容里找不到 JSON 对象 */
	public static final int JSON_OBJECT_NOT_FOUND = 50_024_103;
	/** 返回内容不是合法 JSON */
	public static final int INVALID_JSON = 50_024_104;
	/** 结构化输出失败（尝试次数用尽） */
	public static final int ATTEMPTS_EXHAUSTED = 50_024_105;

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
		super(CODE, message, cause);
		this.lastReply = lastReply;
		this.errors = errors == null ? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(errors));
	}

	private StructuredOutputException(int messageCode, Object[] args, String lastReply, Throwable cause) {
		this(messageCode, args, lastReply, null, cause);
	}

	private StructuredOutputException(int messageCode, Object[] args, String lastReply, List<String> errors,
			Throwable cause) {
		super(messageCode, args);
		if (cause != null) {
			initCause(cause);
		}
		this.lastReply = lastReply;
		this.errors = errors == null ? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(errors));
	}

	/**
	 * 国际化构造入口：文案在读取时按当前语言从资源文件解析。
	 *
	 * @param messageCode 具体错误码（本类的 {@code REPLY_EMPTY} 等常量）
	 * @param lastReply   最后一次原始回复，可为 null
	 * @param cause       原始异常，可为 null
	 * @param args        文案占位参数
	 */
	public static StructuredOutputException of(int messageCode, String lastReply, Throwable cause, Object... args) {
		return new StructuredOutputException(messageCode, args, lastReply, cause);
	}

	/**
	 * 国际化构造入口（携带校验错误清单）：文案在读取时按当前语言从资源文件解析。
	 *
	 * @param messageCode 具体错误码（本类的 {@code ATTEMPTS_EXHAUSTED} 等常量）
	 * @param lastReply   最后一次原始回复，可为 null
	 * @param errors      最后一次校验错误清单，可为 null
	 * @param cause       原始异常，可为 null
	 * @param args        文案占位参数
	 */
	public static StructuredOutputException of(int messageCode, String lastReply, List<String> errors, Throwable cause,
			Object... args) {
		return new StructuredOutputException(messageCode, args, lastReply, errors, cause);
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
