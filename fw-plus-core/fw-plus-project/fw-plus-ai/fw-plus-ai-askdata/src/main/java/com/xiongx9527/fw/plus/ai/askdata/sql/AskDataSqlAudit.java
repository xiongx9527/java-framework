package com.xiongx9527.fw.plus.ai.askdata.sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SQL 插件管线的内存审计记录器。
 *
 * <p>每条记录保留 interceptor key 和 phase，供外部审计适配器转换为持久化事件；审计记录不携带
 * 裸连接或可执行数据库对象。</p>
 */
public class AskDataSqlAudit {

	private static final int DEFAULT_RECORD_CAPACITY = 1024;

	private final List<Record> records = new ArrayList<Record>();

	/**
	 * 记录一次插件链处理阶段。
	 *
	 * @param interceptorKey 已校验的插件标识或固定阶段标识
	 * @param phase 当前处理阶段
	 */
	public synchronized void record(String interceptorKey, String phase) {
		record(interceptorKey, phase, "SUCCEEDED", null, null);
	}

	public synchronized void record(String interceptorKey, String phase, String outcome,
			AskDataSqlCommand before, AskDataSqlCommand after) {
		record(interceptorKey, phase, outcome, before, after, null);
	}

	public synchronized void record(String interceptorKey, String phase, String outcome,
			AskDataSqlCommand before, AskDataSqlCommand after, Correlation correlation) {
		while (records.size() >= DEFAULT_RECORD_CAPACITY) {
			records.remove(0);
		}
		records.add(new Record(interceptorKey, phase, outcome, before, after, correlation));
	}

	/**
	 * 获取当前审计记录的不可修改快照。
	 *
	 * @return 按记录顺序排列的审计记录快照
	 */
	public synchronized List<Record> getRecords() {
		return Collections.unmodifiableList(new ArrayList<Record>(records));
	}

	public static final class Record {

		private final String interceptorKey;
		private final String phase;
		private final String outcome;
		private final boolean sqlChanged;
		private final List<String> parameterNames;
		private final List<String> addedParameterNames;
		private final String requestId;
		private final String conversationId;
		private final String authorizationVersion;

		private Record(String interceptorKey, String phase, String outcome,
				AskDataSqlCommand before, AskDataSqlCommand after, Correlation correlation) {
			if (interceptorKey == null || interceptorKey.trim().length() == 0) {
				throw new IllegalArgumentException("interceptorKey 不能为空白");
			}
			if (phase == null || phase.trim().length() == 0) {
				throw new IllegalArgumentException("phase 不能为空白");
			}
			if (outcome == null || outcome.trim().length() == 0) {
				throw new IllegalArgumentException("outcome 不能为空白");
			}
			this.interceptorKey = interceptorKey.trim();
			this.phase = phase.trim();
			this.outcome = outcome.trim();
			this.sqlChanged = before != null && after != null && !before.getSql().equals(after.getSql());
			this.parameterNames = parameterNames(after == null ? before : after);
			this.addedParameterNames = addedParameterNames(before, after);
			this.requestId = correlation == null ? null : correlation.getRequestId();
			this.conversationId = correlation == null ? null : correlation.getConversationId();
			this.authorizationVersion = correlation == null ? null : correlation.getAuthorizationVersion();
		}

		public String getInterceptorKey() {
			return interceptorKey;
		}

		public String getPhase() {
			return phase;
		}

		public String getOutcome() {
			return outcome;
		}

		public boolean isSqlChanged() {
			return sqlChanged;
		}

		public int getParameterCount() {
			return parameterNames.size();
		}

		public List<String> getParameterNames() {
			return parameterNames;
		}

		public List<String> getAddedParameterNames() {
			return addedParameterNames;
		}

		public String getRequestId() { return requestId; }
		public String getConversationId() { return conversationId; }
		public String getAuthorizationVersion() { return authorizationVersion; }

		@Override
		public String toString() {
			return "Record{" + "interceptorKey='" + interceptorKey + '\'' + ", phase='" + phase + '\''
					+ ", outcome='" + outcome + '\'' + ", sqlChanged=" + sqlChanged
					+ ", parameterNames=" + parameterNames + ", addedParameterNames=" + addedParameterNames + '}';
		}

		private static List<String> parameterNames(AskDataSqlCommand command) {
			if (command == null) {
				return Collections.emptyList();
			}
			List<String> names = new ArrayList<String>();
			for (AskDataSqlParameter parameter : command.getParameters()) {
				names.add(parameter.getName());
			}
			return Collections.unmodifiableList(names);
		}

		private static List<String> addedParameterNames(AskDataSqlCommand before, AskDataSqlCommand after) {
			if (after == null) {
				return Collections.emptyList();
			}
			List<String> names = new ArrayList<String>();
			for (AskDataSqlParameter parameter : after.getParameters()) {
				if (before == null || before.getParameter(parameter.getName()) == null) {
					names.add(parameter.getName());
				}
			}
			return Collections.unmodifiableList(names);
		}
	}

	/** SQL 审计与治理事件共用的脱敏关联标识。 */
	public static final class Correlation {
		private final String requestId;
		private final String conversationId;
		private final String authorizationVersion;
		public Correlation(String requestId, String conversationId, String authorizationVersion) {
			this.requestId = require(requestId, "requestId");
			this.conversationId = require(conversationId, "conversationId");
			this.authorizationVersion = require(authorizationVersion, "authorizationVersion");
		}
		public String getRequestId() { return requestId; }
		public String getConversationId() { return conversationId; }
		public String getAuthorizationVersion() { return authorizationVersion; }
		private static String require(String value, String name) {
			if (value == null || value.trim().length() == 0) {
				throw new IllegalArgumentException(name + " 不能为空白");
			}
			return value.trim();
		}
	}
}
