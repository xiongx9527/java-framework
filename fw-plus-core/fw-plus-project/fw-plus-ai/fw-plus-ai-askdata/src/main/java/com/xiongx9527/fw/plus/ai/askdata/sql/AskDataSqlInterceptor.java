package com.xiongx9527.fw.plus.ai.askdata.sql;

/**
 * AskData SQL 的阶段化改写插件。
 *
 * <p>插件只接收并返回 {@link AskDataSqlCommand}，不能取得裸连接或执行 SQL。任何改写都不能绕过
 * {@link AskDataSqlInterceptorChain} 的最终安全校验。</p>
 */
public interface AskDataSqlInterceptor extends AskDataSqlPlugin {

	/**
	 * 获取插件唯一标识，用于确定同一顺序下的执行顺序与审计记录。
	 *
	 * @return 非空白的插件唯一标识
	 */
	String getKey();

	/**
	 * 获取插件执行顺序，数值越小越早执行。
	 *
	 * <p>不同插件的顺序值可以相同，此时插件链按 {@link #getKey()} 的字典序稳定排序。</p>
	 *
	 * @return 插件执行顺序
	 */
	int getOrder();

	/**
	 * 获取插件参与阶段。旧插件默认位于方言或业务改写阶段。
	 *
	 * @return 命令处理阶段
	 */
	@Override
	default AskDataSqlPhase getPhase() {
		return AskDataSqlPhase.REWRITE;
	}

	/**
	 * 改写 SQL 命令中的 SQL 文本。
	 *
	 * <p>插件不得取得或创建裸 JDBC 连接，不得自行执行 SQL，也不得绕过
	 * {@link AskDataSqlInterceptorChain} 的最终校验。插件输出中的项目与操作人上下文、命名参数、
	 * 授权对象均不可信。插件可以新增不冲突的具名参数；删除参数或以不同值替换同名参数会被拒绝。</p>
	 *
	 * @param command 当前 SQL 命令，不包含数据库连接或执行器
	 * @return 包含改写 SQL 的非空命令；安全字段变更会被插件链覆盖
	 */
	AskDataSqlCommand intercept(AskDataSqlCommand command);
}
