package com.xiongx9527.fw.plus.ai.askdata.agent;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlCommand;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 业务 Agent 执行稳定步骤时接收的不可变中立上下文。
 */
public final class AskDataAgentStepContext {

	private final String stepKey;
	private final AskDataProjectContext projectContext;
	private final String question;
	private final String datasourceKey;
	private final String conversationId;
	private final AskDataMetadata metadata;
	private final List<String> selectedResources;
	private final String prompt;
	private final AskDataSqlCommand sqlCommand;
	private final AskDataSqlResult sqlResult;
	private final int rowLimit;
	private final long timeoutMillis;

	private AskDataAgentStepContext(Builder builder) {
		if (!AskDataAgentStepKey.isSupported(builder.stepKey)) {
			throw new IllegalArgumentException("不支持的 Agent 步骤 key: " + builder.stepKey);
		}
		if (builder.projectContext == null) {
			throw new IllegalArgumentException("projectContext 不能为空");
		}
		if (builder.question == null || builder.question.trim().length() == 0) {
			throw new IllegalArgumentException("question 不能为空白");
		}
		if (builder.rowLimit < 0 || builder.timeoutMillis < 0L) {
			throw new IllegalArgumentException("rowLimit 和 timeoutMillis 不能为负数");
		}
		this.stepKey = builder.stepKey.trim();
		this.projectContext = builder.projectContext;
		this.question = builder.question.trim();
		this.datasourceKey = normalize(builder.datasourceKey);
		this.conversationId = normalize(builder.conversationId);
		this.metadata = builder.metadata;
		this.selectedResources = immutableResources(builder.selectedResources);
		this.prompt = builder.prompt;
		this.sqlCommand = builder.sqlCommand;
		this.sqlResult = builder.sqlResult;
		this.rowLimit = builder.rowLimit;
		this.timeoutMillis = builder.timeoutMillis;
	}

	public static Builder builder(String stepKey, AskDataProjectContext projectContext, String question) {
		return new Builder(stepKey, projectContext, question);
	}

	public String getStepKey() {
		return stepKey;
	}

	public AskDataProjectContext getProjectContext() {
		return projectContext;
	}

	public String getQuestion() {
		return question;
	}

	public String getDatasourceKey() {
		return datasourceKey;
	}

	public String getConversationId() {
		return conversationId;
	}

	public AskDataMetadata getMetadata() {
		return metadata;
	}

	public List<String> getSelectedResources() {
		return selectedResources;
	}

	public String getPrompt() {
		return prompt;
	}

	public AskDataSqlCommand getSqlCommand() {
		return sqlCommand;
	}

	public AskDataSqlResult getSqlResult() {
		return sqlResult;
	}

	public int getRowLimit() {
		return rowLimit;
	}

	public long getTimeoutMillis() {
		return timeoutMillis;
	}

	private static List<String> immutableResources(List<String> resources) {
		if (resources == null || resources.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> copy = new ArrayList<String>(resources.size());
		for (String resource : resources) {
			if (resource == null || resource.trim().length() == 0) {
				throw new IllegalArgumentException("selectedResources 不能包含空白值");
			}
			copy.add(resource.trim());
		}
		return Collections.unmodifiableList(copy);
	}

	private static String normalize(String value) {
		return value == null || value.trim().length() == 0 ? null : value.trim();
	}

	public static final class Builder {

		private final String stepKey;
		private final AskDataProjectContext projectContext;
		private final String question;
		private String datasourceKey;
		private String conversationId;
		private AskDataMetadata metadata;
		private List<String> selectedResources = Collections.emptyList();
		private String prompt;
		private AskDataSqlCommand sqlCommand;
		private AskDataSqlResult sqlResult;
		private int rowLimit;
		private long timeoutMillis;

		private Builder(String stepKey, AskDataProjectContext projectContext, String question) {
			this.stepKey = stepKey;
			this.projectContext = projectContext;
			this.question = question;
		}

		public Builder datasourceKey(String value) {
			this.datasourceKey = value;
			return this;
		}

		public Builder conversationId(String value) {
			this.conversationId = value;
			return this;
		}

		public Builder metadata(AskDataMetadata value) {
			this.metadata = value;
			return this;
		}

		public Builder selectedResources(List<String> value) {
			this.selectedResources = value;
			return this;
		}

		public Builder prompt(String value) {
			this.prompt = value;
			return this;
		}

		public Builder sqlCommand(AskDataSqlCommand value) {
			this.sqlCommand = value;
			return this;
		}

		public Builder sqlResult(AskDataSqlResult value) {
			this.sqlResult = value;
			return this;
		}

		public Builder rowLimit(int value) {
			this.rowLimit = value;
			return this;
		}

		public Builder timeoutMillis(long value) {
			this.timeoutMillis = value;
			return this;
		}

		public AskDataAgentStepContext build() {
			return new AskDataAgentStepContext(this);
		}
	}
}
