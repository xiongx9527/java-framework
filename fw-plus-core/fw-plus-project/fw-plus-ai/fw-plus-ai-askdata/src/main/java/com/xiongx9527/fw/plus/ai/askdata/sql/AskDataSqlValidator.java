package com.xiongx9527.fw.plus.ai.askdata.sql;

/**
 * AskData SQL 安全校验器 SPI。
 *
 * <p>具体 SQL Parser 与方言策略由实现提供；Core 仅定义统一结果和失败分类。插件链始终对最终 SQL
 * 执行此校验，插件不能用改写结果绕过安全边界。</p>
 */
public interface AskDataSqlValidator {

	/**
	 * 校验 SQL 命令是否符合当前安全策略。
	 *
	 * <p>实现应基于命令中的 SQL、项目上下文、命名参数及授权对象进行判定；返回拒绝结果而非
	 * {@code null}，以便插件链保留明确的失败分类。</p>
	 *
	 * @param command 待校验的最终 SQL 命令
	 * @return 非空的校验结果
	 */
	Result validate(AskDataSqlCommand command);

	enum FailureCategory {
		EMPTY_SQL,
		MULTI_STATEMENT,
		WRITE_SQL,
		UNAUTHORIZED_OBJECT,
		INVALID_PARAMETER,
		PARSE_ERROR,
		POLICY_DENIED
	}

	final class Result {

		private static final Result SUCCESS = new Result(true, null, null);

		private final boolean accepted;
		private final FailureCategory failureCategory;
		private final String message;

		private Result(boolean accepted, FailureCategory failureCategory, String message) {
			this.accepted = accepted;
			this.failureCategory = failureCategory;
			this.message = message;
		}

		public static Result success() {
			return SUCCESS;
		}

		public static Result failure(FailureCategory failureCategory, String message) {
			if (failureCategory == null) {
				throw new IllegalArgumentException("failureCategory 不能为空");
			}
			if (message == null || message.trim().length() == 0) {
				throw new IllegalArgumentException("message 不能为空白");
			}
			return new Result(false, failureCategory, message.trim());
		}

		public boolean isAccepted() {
			return accepted;
		}

		public FailureCategory getFailureCategory() {
			return failureCategory;
		}

		public String getMessage() {
			return message;
		}
	}
}
