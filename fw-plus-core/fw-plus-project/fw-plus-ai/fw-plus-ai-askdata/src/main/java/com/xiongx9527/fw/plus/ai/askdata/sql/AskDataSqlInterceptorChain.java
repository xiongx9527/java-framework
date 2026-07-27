package com.xiongx9527.fw.plus.ai.askdata.sql;

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

	private final List<AskDataSqlInterceptor> interceptors;
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
		for (AskDataSqlInterceptor interceptor : interceptors) {
			AskDataSqlCommand rewritten = interceptor.intercept(current);
			if (rewritten == null) {
				throw new IllegalArgumentException("SQL 插件不能返回 null: " + interceptor.getKey());
			}
			current = command.withSql(rewritten.getSql());
			audit.record(interceptor.getKey(), "INTERCEPT");
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

	private static List<AskDataSqlInterceptor> sortAndValidate(List<AskDataSqlInterceptor> source) {
		List<AskDataSqlInterceptor> copy = new ArrayList<AskDataSqlInterceptor>(source.size());
		Set<String> keys = new HashSet<String>();
		for (AskDataSqlInterceptor interceptor : source) {
			if (interceptor == null || interceptor.getKey() == null || interceptor.getKey().trim().length() == 0) {
				throw new IllegalArgumentException("interceptor key 不能为空白");
			}
			if (!keys.add(interceptor.getKey().trim())) {
				throw new IllegalArgumentException("interceptor key 重复: " + interceptor.getKey());
			}
			copy.add(interceptor);
		}
		Collections.sort(copy, new Comparator<AskDataSqlInterceptor>() {
			@Override
			public int compare(AskDataSqlInterceptor left, AskDataSqlInterceptor right) {
				int order = left.getOrder() < right.getOrder() ? -1 : (left.getOrder() == right.getOrder() ? 0 : 1);
				return order != 0 ? order : left.getKey().compareTo(right.getKey());
			}
		});
		return Collections.unmodifiableList(copy);
	}
}
