package com.xiongx9527.fw.plus.ai.askdata.api;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelSelection;
import com.xiongx9527.fw.plus.ai.askdata.publishing.AskDataPublishRequest;

/**
 * AskData 的核心查询请求。
 *
 * <p>请求将自然语言问题与项目、操作人上下文一起传递，确保二次开发的执行器能够以相同边界完成资源隔离、权限校验和审计。
 * 本对象不承载具体模型、Spring Bean 或数据源实现配置，相关扩展应在 Core API 外部适配。</p>
 */
public class AskDataRequest {

	private final AskDataProjectContext projectContext;
	private final String question;
	private final String datasourceKey;
	private final String dialect;
	private final AskDataModelSelection chatModelSelection;
	private final AskDataModelSelection embeddingModelSelection;
	private final Boolean execute;
	private final Integer maxRows;
	private final AskDataPublishRequest publishRequest;

	/**
	 * 创建数据问答请求。
	 *
	 * <p>{@code projectContext} 为 {@code null} 时拒绝；{@code question} 为 {@code null}、空字符串或只包含空白字符时拒绝。
	 * 拒绝空白值可避免请求在缺少隔离边界或业务意图时被二次开发实现误处理。</p>
	 *
	 * @param projectContext 项目与操作人上下文，不能为空
	 * @param question 用户问题，不能为空白
	 * @throws IllegalArgumentException 当上下文或问题为空、空白时抛出
	 */
	public AskDataRequest(AskDataProjectContext projectContext, String question) {
		this(projectContext, question, null, null, AskDataModelSelection.empty(),
				AskDataModelSelection.empty(), null, null, null);
	}

	public AskDataRequest(AskDataProjectContext projectContext, String question, String datasourceKey, String dialect,
			AskDataModelSelection chatModelSelection, AskDataModelSelection embeddingModelSelection,
			Boolean execute, Integer maxRows, AskDataPublishRequest publishRequest) {
		if (projectContext == null) {
			throw new IllegalArgumentException("projectContext 不能为空");
		}
		if (maxRows != null && maxRows.intValue() <= 0) {
			throw new IllegalArgumentException("maxRows 必须大于 0");
		}
		this.projectContext = projectContext;
		this.question = requireNonBlank(question, "question");
		this.datasourceKey = normalize(datasourceKey);
		this.dialect = normalize(dialect);
		this.chatModelSelection = chatModelSelection == null ? AskDataModelSelection.empty() : chatModelSelection;
		this.embeddingModelSelection = embeddingModelSelection == null
				? AskDataModelSelection.empty() : embeddingModelSelection;
		this.execute = execute;
		this.maxRows = maxRows;
		this.publishRequest = publishRequest;
	}

	public static Builder builder(AskDataProjectContext projectContext, String question) {
		return new Builder(projectContext, question);
	}

	/**
	 * 获取项目与操作人上下文。
	 *
	 * @return 不可为空的隔离上下文
	 */
	public AskDataProjectContext getProjectContext() {
		return projectContext;
	}

	/**
	 * 获取已去除首尾空白的用户问题。
	 *
	 * @return 非空白问题
	 */
	public String getQuestion() {
		return question;
	}

	public String getDatasourceKey() { return datasourceKey; }
	public String getDialect() { return dialect; }
	public AskDataModelSelection getChatModelSelection() { return chatModelSelection; }
	public AskDataModelSelection getEmbeddingModelSelection() { return embeddingModelSelection; }
	public Boolean getExecute() { return execute; }
	public Integer getMaxRows() { return maxRows; }
	public AskDataPublishRequest getPublishRequest() { return publishRequest; }

	private static String normalize(String value) {
		return value == null || value.trim().length() == 0 ? null : value.trim();
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

	public static final class Builder {
		private final AskDataProjectContext projectContext;
		private final String question;
		private String datasourceKey;
		private String dialect;
		private AskDataModelSelection chatModelSelection = AskDataModelSelection.empty();
		private AskDataModelSelection embeddingModelSelection = AskDataModelSelection.empty();
		private Boolean execute;
		private Integer maxRows;
		private AskDataPublishRequest publishRequest;

		private Builder(AskDataProjectContext projectContext, String question) {
			this.projectContext = projectContext;
			this.question = question;
		}

		public Builder datasourceKey(String value) { this.datasourceKey = value; return this; }
		public Builder dialect(String value) { this.dialect = value; return this; }
		public Builder chatModelSelection(AskDataModelSelection value) { this.chatModelSelection = value; return this; }
		public Builder embeddingModelSelection(AskDataModelSelection value) { this.embeddingModelSelection = value; return this; }
		public Builder execute(Boolean value) { this.execute = value; return this; }
		public Builder maxRows(Integer value) { this.maxRows = value; return this; }
		public Builder publishRequest(AskDataPublishRequest value) { this.publishRequest = value; return this; }
		public AskDataRequest build() {
			return new AskDataRequest(projectContext, question, datasourceKey, dialect, chatModelSelection,
					embeddingModelSelection, execute, maxRows, publishRequest);
		}
	}
}
