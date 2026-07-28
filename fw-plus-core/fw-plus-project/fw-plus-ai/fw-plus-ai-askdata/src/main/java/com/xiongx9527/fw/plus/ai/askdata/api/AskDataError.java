package com.xiongx9527.fw.plus.ai.askdata.api;

/**
 * AskData 的受控失败信息。
 *
 * <p>错误对象仅表达稳定的业务错误码和面向调用方的说明，避免将底层模型、Spring 或数据访问异常泄漏到 Core API。
 * 二次开发实现可扩展错误码集合，但必须保留项目与操作人隔离边界，不能以错误信息替代权限判断。</p>
 */
public class AskDataError {

	private final String code;
	private final String message;

	/**
	 * 创建受控失败信息。
	 *
	 * <p>{@code code} 与 {@code message} 均拒绝 {@code null}、空字符串和只包含空白字符的值；
	 * 统一的非空白语义保证调用方可稳定地识别失败原因。</p>
	 *
	 * @param code 稳定的业务错误码，不能为空白
	 * @param message 面向调用方的错误说明，不能为空白
	 * @throws IllegalArgumentException 当错误码或说明为空、空白时抛出
	 */
	public AskDataError(String code, String message) {
		this.code = requireNonBlank(code, "code");
		this.message = requireNonBlank(message, "message");
	}

	/**
	 * 获取稳定的业务错误码。
	 *
	 * @return 非空白错误码
	 */
	public String getCode() {
		return code;
	}

	/**
	 * 获取面向调用方的错误说明。
	 *
	 * @return 非空白错误说明
	 */
	public String getMessage() {
		return message;
	}

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null) {
			throw new IllegalArgumentException(fieldName + " 不能为空白");
		}
		String normalizedValue = trimWhitespace(value);
		if (normalizedValue.length() == 0) {
			throw new IllegalArgumentException(fieldName + " 不能为空白");
		}
		return normalizedValue;
	}

	private static String trimWhitespace(String value) {
		int beginIndex = 0;
		int endIndex = value.length();
		while (beginIndex < endIndex && isWhitespace(value.charAt(beginIndex))) {
			beginIndex++;
		}
		while (beginIndex < endIndex && isWhitespace(value.charAt(endIndex - 1))) {
			endIndex--;
		}
		return value.substring(beginIndex, endIndex);
	}

	private static boolean isWhitespace(char character) {
		return Character.isWhitespace(character) || Character.isSpaceChar(character);
	}
}
