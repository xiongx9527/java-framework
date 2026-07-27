package com.xiongx9527.fw.plus.ai.askdata.sql;

/**
 * AskData SQL 的阶段化改写插件。
 *
 * <p>插件只接收并返回 {@link AskDataSqlCommand}，不能取得裸连接或执行 SQL。任何改写都不能绕过
 * {@link AskDataSqlInterceptorChain} 的最终安全校验。</p>
 */
public interface AskDataSqlInterceptor {

	String getKey();

	int getOrder();

	AskDataSqlCommand intercept(AskDataSqlCommand command);
}
