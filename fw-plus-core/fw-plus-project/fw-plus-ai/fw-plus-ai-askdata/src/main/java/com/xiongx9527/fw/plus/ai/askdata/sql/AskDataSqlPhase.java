package com.xiongx9527.fw.plus.ai.askdata.sql;

/**
 * SQL 插件参与受控管线的阶段。
 *
 * <p>命令阶段严格按枚举声明顺序执行；结果映射只能在受限执行完成后由调用方显式触发。</p>
 */
public enum AskDataSqlPhase {

	ASSEMBLE,
	BIND_PARAMETERS,
	REWRITE,
	PREPARE,
	RESULT_MAP
}
