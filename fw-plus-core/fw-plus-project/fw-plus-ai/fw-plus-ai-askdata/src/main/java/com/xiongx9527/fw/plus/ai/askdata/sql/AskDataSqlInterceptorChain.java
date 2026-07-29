package com.xiongx9527.fw.plus.ai.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataRowPredicate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * AskData SQL 插件链。
 *
 * <p>插件按 {@code order} 升序、key 升序执行。Core 不暴露裸连接；所有插件完成后必须校验最终 SQL，
 * 因而任意插件改写都不能绕过多语句、写入权限或对象授权等安全策略。</p>
 */
public final class AskDataSqlInterceptorChain {

	private final List<InterceptorNode> interceptors;
	private final AskDataSqlValidator validator;
	private final AskDataSqlAudit audit;

	public AskDataSqlInterceptorChain(List<AskDataSqlInterceptor> interceptors, AskDataSqlValidator validator,
			AskDataSqlAudit audit) {
		if (interceptors == null || validator == null || audit == null) {
			throw new IllegalArgumentException("interceptors、validator 和 audit 不能为空");
		}
		this.interceptors = sortAndValidate(interceptors);
		this.validator = validator;
		this.audit = audit;
	}

	public AskDataSqlCommand execute(AskDataSqlCommand command) {
		if (command == null) {
			throw new IllegalArgumentException("command 不能为空");
		}
		AskDataSqlCommand current = command;
		for (InterceptorNode interceptor : interceptors) {
			AskDataSqlCommand rewritten = interceptor.interceptor.intercept(current);
			if (rewritten == null) {
				throw new IllegalArgumentException("SQL 插件不能返回 null: " + interceptor.key);
			}
			validateAuthorizationBoundary(current.getAuthorizationSummary(), rewritten.getAuthorizationSummary());
			current = command.withSql(rewritten.getSql());
			audit.record(interceptor.key, "INTERCEPT");
		}
		audit.record("validator", "FINAL_VALIDATE");
		AskDataSqlValidator.Result result = validator.validate(current);
		if (result == null) {
			throw new IllegalArgumentException("SQL 校验器不能返回 null");
		}
		if (!result.isAccepted()) {
			throw new IllegalArgumentException(result.getFailureCategory().name() + ": " + result.getMessage());
		}
		return current;
	}

	private static void validateAuthorizationBoundary(AskDataAuthorizationSummary original,
			AskDataAuthorizationSummary rewritten) {
		if (original == null) {
			return;
		}
		if (rewritten == null || !original.getAuthorizedObjects().containsAll(rewritten.getAuthorizedObjects())
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

	private static List<InterceptorNode> sortAndValidate(List<AskDataSqlInterceptor> source) {
		List<InterceptorNode> copy = new ArrayList<InterceptorNode>(source.size());
		Set<String> keys = new HashSet<String>();
		for (AskDataSqlInterceptor interceptor : source) {
			if (interceptor == null) {
				throw new IllegalArgumentException("interceptor key 不能为空白");
			}
			String key = interceptor.getKey();
			if (key == null || key.trim().length() == 0) {
				throw new IllegalArgumentException("interceptor key 不能为空白");
			}
			String trimmedKey = key.trim();
			if (!keys.add(trimmedKey)) {
				throw new IllegalArgumentException("interceptor key 重复: " + trimmedKey);
			}
			copy.add(new InterceptorNode(interceptor, trimmedKey, interceptor.getOrder()));
		}
		Collections.sort(copy, new Comparator<InterceptorNode>() {
			@Override
			public int compare(InterceptorNode left, InterceptorNode right) {
				int order = left.order < right.order ? -1 : (left.order == right.order ? 0 : 1);
				return order != 0 ? order : left.key.compareTo(right.key);
			}
		});
		return Collections.unmodifiableList(copy);
	}

	private static final class InterceptorNode {

		private final AskDataSqlInterceptor interceptor;
		private final String key;
		private final int order;

		private InterceptorNode(AskDataSqlInterceptor interceptor, String key, int order) {
			this.interceptor = interceptor;
			this.key = key;
			this.order = order;
		}
	}
}
