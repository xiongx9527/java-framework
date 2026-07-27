package com.xiongx9527.fw.plus.ai.askdata.api;

/**
 * AskData 的核心查询结果。
 *
 * <p>结果不暴露模型、SQL 或 Spring 类型，便于二次开发在不同执行引擎之间保持稳定调用方式。
 * {@link #getError()} 为 {@code null} 表示成功；非空表示受控失败，调用方可依据错误码处理。</p>
 */
public class AskDataResult {

	private final String answer;
	private final AskDataError error;

	/**
	 * 创建查询结果。
	 *
	 * <p>错误对象为 {@code null} 时构造成功结果；错误对象非空时构造失败结果。成功答案允许为 {@code null} 或空白，
	 * 以支持没有文本答案但仍成功完成的二次开发场景。</p>
	 *
	 * @param answer 面向调用方的答案，可为空
	 * @param error 失败错误；为空表示成功
	 */
	public AskDataResult(String answer, AskDataError error) {
		this.answer = answer;
		this.error = error;
	}

	/**
	 * 创建成功结果。
	 *
	 * @param answer 面向调用方的答案，可为空
	 * @return 成功结果
	 */
	public static AskDataResult success(String answer) {
		return new AskDataResult(answer, null);
	}

	/**
	 * 创建失败结果。
	 *
	 * @param error 受控错误，不能为空
	 * @return 失败结果
	 * @throws IllegalArgumentException 当错误对象为空时抛出
	 */
	public static AskDataResult failure(AskDataError error) {
		if (error == null) {
			throw new IllegalArgumentException("error 不能为空");
		}
		return new AskDataResult(null, error);
	}

	/**
	 * 判断查询是否成功。
	 *
	 * @return 没有错误对象时返回 {@code true}
	 */
	public boolean isSuccess() {
		return error == null;
	}

	/**
	 * 获取面向调用方的答案。
	 *
	 * @return 答案，可为空
	 */
	public String getAnswer() {
		return answer;
	}

	/**
	 * 获取受控失败错误。
	 *
	 * @return 失败时的错误；成功时为 {@code null}
	 */
	public AskDataError getError() {
		return error;
	}
}
