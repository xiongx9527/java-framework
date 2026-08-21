package com.xiongx9527.fw.plus.ai.askdata.model;

/** 一次模型调用的不可变、脱敏路由与终态记录。 */
public final class AskDataModelAttempt {

	public enum ModelType { CHAT, EMBEDDING }
	public enum State { ATTEMPTED, SUCCEEDED, FAILED }

	private final ModelType modelType;
	private final String modelKey;
	private final AskDataModelRoutePolicy routePolicy;
	private final String routeReason;
	private final State state;
	private final String failureCode;
	private final String switchReason;
	private final String actualModelKey;
	private final AskDataModelUsage usage;

	public AskDataModelAttempt(ModelType modelType, String modelKey, AskDataModelRoutePolicy routePolicy,
			String routeReason, State state, String failureCode, String switchReason, String actualModelKey,
			AskDataModelUsage usage) {
		if (modelType == null || blank(modelKey) || routePolicy == null || blank(routeReason) || state == null) {
			throw new IllegalArgumentException("modelType、modelKey、routePolicy、routeReason 和 state 不能为空");
		}
		if (state == State.FAILED && blank(failureCode)) {
			throw new IllegalArgumentException("失败模型尝试必须携带 failureCode");
		}
		if (state == State.SUCCEEDED && blank(actualModelKey)) {
			throw new IllegalArgumentException("成功模型尝试必须携带 actualModelKey");
		}
		this.modelType = modelType;
		this.modelKey = modelKey.trim();
		this.routePolicy = routePolicy;
		this.routeReason = routeReason.trim();
		this.state = state;
		this.failureCode = normalize(failureCode);
		this.switchReason = normalize(switchReason);
		this.actualModelKey = normalize(actualModelKey);
		this.usage = usage == null ? AskDataModelUsage.unknown() : usage;
	}

	public ModelType getModelType() { return modelType; }
	public String getModelKey() { return modelKey; }
	public AskDataModelRoutePolicy getRoutePolicy() { return routePolicy; }
	public String getRouteReason() { return routeReason; }
	public State getState() { return state; }
	public String getFailureCode() { return failureCode; }
	public String getSwitchReason() { return switchReason; }
	public String getActualModelKey() { return actualModelKey; }
	public AskDataModelUsage getUsage() { return usage; }

	private static String normalize(String value) { return blank(value) ? null : value.trim(); }
	private static boolean blank(String value) { return value == null || value.trim().length() == 0; }
}
