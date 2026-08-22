package com.xiongx9527.fw.plus.ai.askdata.sql;

/**
 * 在受限 SQL 执行完成后映射结构化结果的插件。
 */
public interface AskDataSqlResultMapper extends AskDataSqlPlugin {

	@Override
	default AskDataSqlPhase getPhase() {
		return AskDataSqlPhase.RESULT_MAP;
	}

	/**
	 * 映射结构化列和行。返回结果中的执行事实必须与输入一致。
	 *
	 * @param command 已通过最终安全校验并完成执行的命令
	 * @param result 受限执行结果
	 * @return 映射后的非空结果
	 */
	AskDataSqlResult map(AskDataSqlCommand command, AskDataSqlResult result);
}
