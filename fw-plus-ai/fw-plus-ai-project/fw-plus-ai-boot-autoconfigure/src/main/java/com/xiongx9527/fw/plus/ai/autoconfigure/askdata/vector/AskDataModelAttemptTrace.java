package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;

/** 单次模型调用的脱敏路由轨迹。 */
public final class AskDataModelAttemptTrace {

    private final String modelKey;
    private final AskDataModelRoutePolicy routePolicy;
    private final AskDataEvent.ModelState state;
    private final String failureCode;
    private final String switchReason;
    private final String actualModelKey;

    private AskDataModelAttemptTrace(String modelKey, AskDataModelRoutePolicy routePolicy,
            AskDataEvent.ModelState state, String failureCode, String switchReason, String actualModelKey) {
        if (isBlank(modelKey) || routePolicy == null || state == null) {
            throw new IllegalArgumentException("modelKey、routePolicy 和 state 不能为空");
        }
        if (state == AskDataEvent.ModelState.FAILED && isBlank(failureCode)) {
            throw new IllegalArgumentException("失败模型轨迹必须携带 failureCode");
        }
        this.modelKey = modelKey.trim();
        this.routePolicy = routePolicy;
        this.state = state;
        this.failureCode = normalize(failureCode);
        this.switchReason = normalize(switchReason);
        this.actualModelKey = normalize(actualModelKey);
    }

    public static AskDataModelAttemptTrace attempted(String modelKey, AskDataModelRoutePolicy routePolicy) {
        return new AskDataModelAttemptTrace(modelKey, routePolicy, AskDataEvent.ModelState.ATTEMPTED,
                null, null, null);
    }

    public static AskDataModelAttemptTrace succeeded(String modelKey, AskDataModelRoutePolicy routePolicy,
            String actualModelKey) {
        if (isBlank(actualModelKey)) {
            throw new IllegalArgumentException("成功模型轨迹必须携带 actualModelKey");
        }
        return new AskDataModelAttemptTrace(modelKey, routePolicy, AskDataEvent.ModelState.SUCCEEDED,
                null, null, actualModelKey);
    }

    public static AskDataModelAttemptTrace failed(String modelKey, AskDataModelRoutePolicy routePolicy,
            String failureCode, String switchReason, String actualModelKey) {
        return new AskDataModelAttemptTrace(modelKey, routePolicy, AskDataEvent.ModelState.FAILED,
                failureCode, switchReason, actualModelKey);
    }

    public String getModelKey() { return modelKey; }
    public AskDataModelRoutePolicy getRoutePolicy() { return routePolicy; }
    public AskDataEvent.ModelState getState() { return state; }
    public String getFailureCode() { return failureCode; }
    public String getSwitchReason() { return switchReason; }
    public String getActualModelKey() { return actualModelKey; }

    private static String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
