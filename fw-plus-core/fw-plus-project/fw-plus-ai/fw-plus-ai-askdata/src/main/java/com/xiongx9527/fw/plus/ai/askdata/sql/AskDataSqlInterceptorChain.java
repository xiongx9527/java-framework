package com.xiongx9527.fw.plus.ai.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataRowPredicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * AskData SQL 阶段化插件链。
 *
 * <p>命令插件按阶段、{@code order}、能力 key 稳定执行，结果插件在受限执行完成后单独调用。
 * 插件新增的非冲突具名参数会传给后续阶段；删除或替换参数会 fail-closed。所有命令插件完成后仍会执行
 * 参数绑定完整性和最终安全校验。</p>
 */
public final class AskDataSqlInterceptorChain {

	private final List<InterceptorNode> interceptors;
	private final List<ResultMapperNode> resultMappers;
	private final AskDataSqlValidator validator;
	private final AskDataSqlAudit audit;

	public AskDataSqlInterceptorChain(List<AskDataSqlInterceptor> interceptors, AskDataSqlValidator validator,
			AskDataSqlAudit audit) {
		this(interceptors, Collections.<AskDataSqlResultMapper>emptyList(), validator, audit);
	}

	public AskDataSqlInterceptorChain(List<AskDataSqlInterceptor> interceptors,
			List<AskDataSqlResultMapper> resultMappers, AskDataSqlValidator validator, AskDataSqlAudit audit) {
		if (interceptors == null || resultMappers == null || validator == null || audit == null) {
			throw new IllegalArgumentException("interceptors、resultMappers、validator 和 audit 不能为空");
		}
		Set<String> keys = new HashSet<String>();
		this.interceptors = sortAndValidateInterceptors(interceptors, keys);
		this.resultMappers = sortAndValidateResultMappers(resultMappers, keys);
		this.validator = validator;
		this.audit = audit;
	}

	public AskDataSqlCommand execute(AskDataSqlCommand command) {
		return execute(command, null);
	}

	public AskDataSqlCommand execute(AskDataSqlCommand command, AskDataSqlAudit.Correlation correlation) {
		if (command == null) {
			throw new IllegalArgumentException("command 不能为空");
		}
		AskDataSqlCommand current = command;
		String lastPluginKey = null;
		for (InterceptorNode node : interceptors) {
			AskDataSqlCommand before = current;
			AskDataSqlCommand rewritten = null;
			try {
				rewritten = node.interceptor.intercept(before);
				if (rewritten == null) {
					throw new IllegalArgumentException("SQL 插件不能返回 null");
				}
				validateAuthorizationBoundary(before.getAuthorizationSummary(), rewritten.getAuthorizationSummary());
				AskDataAuthorizationSummary authorizationSummary = rewritten.getAuthorizationSummary();
				current = new AskDataSqlCommand(before.getContext(), rewritten.getSql(),
						mergeParameters(before, rewritten), authorizedObjects(before, authorizationSummary),
						authorizationSummary);
				audit.record(node.key, node.phase.name(), "SUCCEEDED", before, current, correlation);
				lastPluginKey = node.key;
			} catch (RuntimeException ex) {
				audit.record(node.key, node.phase.name(), "FAILED", before, rewritten, correlation);
				throw pluginFailure(node.key, node.phase, ex);
			}
		}
		validateFinalCommand(current, lastPluginKey, correlation);
		return current;
	}

	/**
	 * 在 SQL 受限执行完成后映射结构化结果。
	 */
	public AskDataSqlResult mapResult(AskDataSqlCommand command, AskDataSqlResult result) {
		return mapResult(command, result, null);
	}

	public AskDataSqlResult mapResult(AskDataSqlCommand command, AskDataSqlResult result,
			AskDataSqlAudit.Correlation correlation) {
		if (command == null || result == null) {
			throw new IllegalArgumentException("command 和 result 不能为空");
		}
		AskDataSqlResult current = result;
		for (ResultMapperNode node : resultMappers) {
			try {
				AskDataSqlResult mapped = node.mapper.map(command, current);
				if (mapped == null) {
					throw new IllegalArgumentException("SQL 结果映射插件不能返回 null");
				}
				validateExecutionFacts(current, mapped);
				current = mapped;
				audit.record(node.key, AskDataSqlPhase.RESULT_MAP.name(), "SUCCEEDED", command, command, correlation);
			} catch (RuntimeException ex) {
				audit.record(node.key, AskDataSqlPhase.RESULT_MAP.name(), "FAILED", command, command, correlation);
				throw pluginFailure(node.key, AskDataSqlPhase.RESULT_MAP, ex);
			}
		}
		return current;
	}

	private void validateFinalCommand(AskDataSqlCommand command, String lastPluginKey,
			AskDataSqlAudit.Correlation correlation) {
		try {
			validateNamedParameterBindings(command);
			AskDataSqlValidator.Result result = validator.validate(command);
			if (result == null) {
				throw new IllegalArgumentException("SQL 校验器不能返回 null");
			}
			if (!result.isAccepted()) {
				throw new IllegalArgumentException(result.getFailureCategory().name() + ": " + result.getMessage());
			}
			audit.record("validator", "FINAL_VALIDATE", "SUCCEEDED", command, command, correlation);
		} catch (RuntimeException ex) {
			audit.record(lastPluginKey == null ? "validator" : lastPluginKey,
					"FINAL_VALIDATE", "FAILED", command, command, correlation);
			throw ex;
		}
	}

	private static List<AskDataSqlParameter> mergeParameters(AskDataSqlCommand before,
			AskDataSqlCommand rewritten) {
		List<AskDataSqlParameter> merged = new ArrayList<AskDataSqlParameter>(before.getParameters());
		for (AskDataSqlParameter required : before.getParameters()) {
			AskDataSqlParameter candidate = rewritten.getParameter(required.getName());
			if (candidate == null) {
				throw new IllegalArgumentException("SQL 插件不能删除命名参数: " + required.getName());
			}
			if (!required.hasSameValue(candidate)) {
				throw new IllegalArgumentException("命名参数冲突: " + required.getName());
			}
		}
		for (AskDataSqlParameter parameter : rewritten.getParameters()) {
			if (before.getParameter(parameter.getName()) == null) {
				merged.add(parameter);
			}
		}
		return merged;
	}

	private static void validateNamedParameterBindings(AskDataSqlCommand command) {
		for (String name : namedParameterNames(command.getSql())) {
			if (command.getParameter(name) == null) {
				throw new IllegalArgumentException("无法绑定命名参数: " + name);
			}
		}
	}

	private static Set<String> namedParameterNames(String sql) {
		Set<String> names = new LinkedHashSet<String>();
		boolean singleQuoted = false;
		boolean doubleQuoted = false;
		boolean backtickQuoted = false;
		boolean lineComment = false;
		boolean blockComment = false;
		for (int index = 0; index < sql.length(); index++) {
			char current = sql.charAt(index);
			char next = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';
			if (lineComment) {
				if (current == '\n' || current == '\r') {
					lineComment = false;
				}
				continue;
			}
			if (blockComment) {
				if (current == '*' && next == '/') {
					blockComment = false;
					index++;
				}
				continue;
			}
			if (!singleQuoted && !doubleQuoted && !backtickQuoted && current == '-' && next == '-') {
				lineComment = true;
				index++;
				continue;
			}
			if (!singleQuoted && !doubleQuoted && !backtickQuoted && current == '/' && next == '*') {
				blockComment = true;
				index++;
				continue;
			}
			if (!doubleQuoted && !backtickQuoted && current == '\'') {
				if (singleQuoted && next == '\'') {
					index++;
				} else {
					singleQuoted = !singleQuoted;
				}
				continue;
			}
			if (!singleQuoted && !backtickQuoted && current == '"') {
				doubleQuoted = !doubleQuoted;
				continue;
			}
			if (!singleQuoted && !doubleQuoted && current == '`') {
				backtickQuoted = !backtickQuoted;
				continue;
			}
			if (singleQuoted || doubleQuoted || backtickQuoted || current != ':' || next == ':'
					|| index > 0 && sql.charAt(index - 1) == ':') {
				continue;
			}
			if (isParameterStart(next)) {
				int end = index + 2;
				while (end < sql.length() && isParameterPart(sql.charAt(end))) {
					end++;
				}
				names.add(sql.substring(index + 1, end));
				index = end - 1;
			}
		}
		return names;
	}

	private static boolean isParameterStart(char value) {
		return Character.isJavaIdentifierStart(value);
	}

	private static boolean isParameterPart(char value) {
		return Character.isJavaIdentifierPart(value);
	}

	private static void validateExecutionFacts(AskDataSqlResult before, AskDataSqlResult after) {
		if (before.getExecutionStatus() != after.getExecutionStatus()
				|| before.getExecutedRowCount() != after.getExecutedRowCount()
				|| before.getRowLimit() != after.getRowLimit()
				|| before.getTimeoutMillis() != after.getTimeoutMillis()) {
			throw new IllegalArgumentException("结果映射插件不能改变 SQL 执行事实");
		}
		int maximumRows = before.getExecutedRowCount();
		if (before.getRowLimit() > 0 && before.getRowLimit() < maximumRows) {
			maximumRows = before.getRowLimit();
		}
		if (after.getRows().size() > maximumRows) {
			throw new IllegalArgumentException("结果映射插件不能扩大受限 SQL 结果行数");
		}
	}

	private static IllegalArgumentException pluginFailure(String key, AskDataSqlPhase phase, RuntimeException cause) {
		String message = cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
		return new IllegalArgumentException("SQL 插件 " + key + " 在 " + phase.name() + " 阶段失败: " + message, cause);
	}

	private static Set<String> authorizedObjects(AskDataSqlCommand command,
			AskDataAuthorizationSummary authorizationSummary) {
		if (authorizationSummary == null) {
			return command.getAuthorizedObjects();
		}
		Set<String> authorizedObjects = new LinkedHashSet<String>();
		for (AskDataAuthorizedObject authorizedObject : authorizationSummary.getAuthorizedObjects()) {
			if (authorizedObject != null) {
				authorizedObjects.add(authorizedObject.getTableName());
			}
		}
		return authorizedObjects;
	}

	private static void validateAuthorizationBoundary(AskDataAuthorizationSummary original,
			AskDataAuthorizationSummary rewritten) {
		if (original == null) {
			return;
		}
		if (rewritten == null || !rewritten.isAllowed() || !original.hasSameBinding(rewritten)
				|| (rewritten.getAuthorizedObjects().isEmpty() && !original.getAuthorizedObjects().isEmpty())
				|| !original.getAuthorizedObjects().containsAll(rewritten.getAuthorizedObjects())
				|| !containsAllRequiredRowPredicates(rewritten, original)) {
			throw new IllegalArgumentException("插件不能扩大或弱化 authorizationSummary");
		}
	}

	private static boolean containsAllRequiredRowPredicates(AskDataAuthorizationSummary candidate,
			AskDataAuthorizationSummary required) {
		for (AskDataRowPredicate requiredPredicate : required.getRowPredicates()) {
			if (!containsRowPredicate(candidate.getRowPredicates(), requiredPredicate)) {
				return false;
			}
		}
		return true;
	}

	private static boolean containsRowPredicate(List<AskDataRowPredicate> candidates, AskDataRowPredicate required) {
		for (AskDataRowPredicate candidate : candidates) {
			if (candidate.getAuthorizedObject().equals(required.getAuthorizedObject())
					&& candidate.getSqlFragment().equals(required.getSqlFragment())
					&& candidate.getParameters().equals(required.getParameters())) {
				return true;
			}
		}
		return false;
	}

	private static List<InterceptorNode> sortAndValidateInterceptors(List<AskDataSqlInterceptor> source,
			Set<String> keys) {
		List<InterceptorNode> copy = new ArrayList<InterceptorNode>(source.size());
		for (AskDataSqlInterceptor interceptor : source) {
			String key = validatePlugin(interceptor, keys);
			AskDataSqlPhase phase = interceptor.getPhase();
			if (phase == null || phase == AskDataSqlPhase.RESULT_MAP) {
				throw new IllegalArgumentException("SQL 命令插件阶段非法: " + key);
			}
			copy.add(new InterceptorNode(interceptor, key, interceptor.getOrder(), phase));
		}
		Collections.sort(copy, new Comparator<InterceptorNode>() {
			@Override
			public int compare(InterceptorNode left, InterceptorNode right) {
				int phase = left.phase.compareTo(right.phase);
				if (phase != 0) {
					return phase;
				}
				int order = compareInts(left.order, right.order);
				return order != 0 ? order : left.key.compareTo(right.key);
			}
		});
		return Collections.unmodifiableList(copy);
	}

	private static List<ResultMapperNode> sortAndValidateResultMappers(List<AskDataSqlResultMapper> source,
			Set<String> keys) {
		List<ResultMapperNode> copy = new ArrayList<ResultMapperNode>(source.size());
		for (AskDataSqlResultMapper mapper : source) {
			String key = validatePlugin(mapper, keys);
			if (mapper.getPhase() != AskDataSqlPhase.RESULT_MAP) {
				throw new IllegalArgumentException("SQL 结果映射插件阶段非法: " + key);
			}
			copy.add(new ResultMapperNode(mapper, key, mapper.getOrder()));
		}
		Collections.sort(copy, new Comparator<ResultMapperNode>() {
			@Override
			public int compare(ResultMapperNode left, ResultMapperNode right) {
				int order = compareInts(left.order, right.order);
				return order != 0 ? order : left.key.compareTo(right.key);
			}
		});
		return Collections.unmodifiableList(copy);
	}

	private static String validatePlugin(AskDataSqlPlugin plugin, Set<String> keys) {
		if (plugin == null) {
			throw new IllegalArgumentException("SQL plugin key 不能为空白");
		}
		String declaredKey = plugin.getKey();
		if (declaredKey == null || declaredKey.trim().length() == 0) {
			throw new IllegalArgumentException("SQL plugin key 不能为空白");
		}
		String key = declaredKey.trim();
		if (!keys.add(key)) {
			throw new IllegalArgumentException("SQL plugin key 重复: " + key);
		}
		return key;
	}

	private static int compareInts(int left, int right) {
		return left < right ? -1 : (left == right ? 0 : 1);
	}

	private static final class InterceptorNode {

		private final AskDataSqlInterceptor interceptor;
		private final String key;
		private final int order;
		private final AskDataSqlPhase phase;

		private InterceptorNode(AskDataSqlInterceptor interceptor, String key, int order, AskDataSqlPhase phase) {
			this.interceptor = interceptor;
			this.key = key;
			this.order = order;
			this.phase = phase;
		}
	}

	private static final class ResultMapperNode {

		private final AskDataSqlResultMapper mapper;
		private final String key;
		private final int order;

		private ResultMapperNode(AskDataSqlResultMapper mapper, String key, int order) {
			this.mapper = mapper;
			this.key = key;
			this.order = order;
		}
	}
}
