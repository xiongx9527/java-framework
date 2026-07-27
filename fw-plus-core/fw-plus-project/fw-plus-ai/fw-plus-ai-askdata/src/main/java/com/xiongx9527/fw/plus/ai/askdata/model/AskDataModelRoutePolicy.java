package com.xiongx9527.fw.plus.ai.askdata.model;

/**
 * 模型路由命中的配置层级。
 *
 * <p>枚举顺序即 Core 的稳定优先级。Java 17 独立适配服务只能提交模型 key 和执行结果，
 * 不得重新解释项目、数据源或任务级配置，以保持 Core 内的路由可审计且可预测。</p>
 */
public enum AskDataModelRoutePolicy {

	REQUEST_MODEL_KEY,
	PROJECT_KEY,
	DATASOURCE_KEY,
	AGENT_TASK_KEY,
	DEFAULT_KEY
}
