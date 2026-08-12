package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRoutePolicy;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelAttempt;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelResult;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelUsage;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;

/** 单次模型调用的脱敏路由轨迹。 */
public final class AskDataModelAttemptTrace {

    private final String modelKey;
    private final AskDataModelRoutePolicy routePolicy;
    private final String routeReason;
    private final AskDataEvent.ModelState state;
    private final String failureCode;
    private final String switchReason;
    private final String actualModelKey;
    private final AskDataModelUsage usage;

    private AskDataModelAttemptTrace(String modelKey, AskDataModelRoutePolicy routePolicy,
            AskDataEvent.ModelState state, String failureCode, String switchReason, String actualModelKey) {
        this(modelKey, routePolicy, routePolicy == null ? null : routePolicy.name(), state, failureCode,
                switchReason, actualModelKey, AskDataModelUsage.unknown());
    }

    private AskDataModelAttemptTrace(String modelKey, AskDataModelRoutePolicy routePolicy, String routeReason,
            AskDataEvent.ModelState state, String failureCode, String switchReason, String actualModelKey,
            AskDataModelUsage usage) {
        if (isBlank(modelKey) || routePolicy == null || isBlank(routeReason) || state == null) {
            throw new IllegalArgumentException("modelKey、routePolicy、routeReason 和 state 不能为空");
        }
        if (state == AskDataEvent.ModelState.FAILED && isBlank(failureCode)) {
            throw new IllegalArgumentException("失败模型轨迹必须携带 failureCode");
        }
        this.modelKey = modelKey.trim();
        this.routePolicy = routePolicy;
        this.routeReason = routeReason.trim();
        this.state = state;
        this.failureCode = normalize(failureCode);
        this.switchReason = normalize(switchReason);
        this.actualModelKey = normalize(actualModelKey);
        this.usage = usage == null ? AskDataModelUsage.unknown() : usage;
    }

    public static AskDataModelAttemptTrace attempted(String modelKey, AskDataModelRoutePolicy routePolicy) {
        return new AskDataModelAttemptTrace(modelKey, routePolicy, AskDataEvent.ModelState.ATTEMPTED,
                null, null, null);
    }

    public static AskDataModelAttemptTrace attempted(AskDataModelResult routed) {
        return fromRouted(routed, AskDataEvent.ModelState.ATTEMPTED, null, null, null,
                AskDataModelUsage.unknown());
    }

    public static AskDataModelAttemptTrace succeeded(String modelKey, AskDataModelRoutePolicy routePolicy,
            String actualModelKey) {
        if (isBlank(actualModelKey)) {
            throw new IllegalArgumentException("成功模型轨迹必须携带 actualModelKey");
        }
        return new AskDataModelAttemptTrace(modelKey, routePolicy, AskDataEvent.ModelState.SUCCEEDED,
                null, null, actualModelKey);
    }

    public static AskDataModelAttemptTrace succeeded(AskDataModelResult routed, String actualModelKey,
            AskDataModelUsage usage) {
        return fromRouted(routed, AskDataEvent.ModelState.SUCCEEDED, null, null, actualModelKey, usage);
    }

    public static AskDataModelAttemptTrace failed(String modelKey, AskDataModelRoutePolicy routePolicy,
            String failureCode, String switchReason, String actualModelKey) {
        return new AskDataModelAttemptTrace(modelKey, routePolicy, AskDataEvent.ModelState.FAILED,
                failureCode, switchReason, actualModelKey);
    }

    public static AskDataModelAttemptTrace failed(AskDataModelResult routed, String failureCode,
            String switchReason, String actualModelKey) {
        return failed(routed, failureCode, switchReason, actualModelKey, AskDataModelUsage.unknown());
    }

    public static AskDataModelAttemptTrace failed(AskDataModelResult routed, String failureCode,
            String switchReason, String actualModelKey, AskDataModelUsage usage) {
        return fromRouted(routed, AskDataEvent.ModelState.FAILED, failureCode, switchReason, actualModelKey,
                usage);
    }

    public String getModelKey() { return modelKey; }
    public AskDataModelRoutePolicy getRoutePolicy() { return routePolicy; }
    public String getRouteReason() { return routeReason; }
    public AskDataEvent.ModelState getState() { return state; }
    public String getFailureCode() { return failureCode; }
    public String getSwitchReason() { return switchReason; }
    public String getActualModelKey() { return actualModelKey; }
    public AskDataModelUsage getUsage() { return usage; }

    public AskDataModelAttempt toCore(AskDataModelAttempt.ModelType modelType) {
        return new AskDataModelAttempt(modelType, modelKey, routePolicy, routeReason,
                AskDataModelAttempt.State.valueOf(state.name()), failureCode, switchReason, actualModelKey, usage);
    }

    private static AskDataModelAttemptTrace fromRouted(AskDataModelResult routed, AskDataEvent.ModelState state,
            String failureCode, String switchReason, String actualModelKey, AskDataModelUsage usage) {
        if (routed == null || !routed.isRouted()) {
            throw new IllegalArgumentException("routed 必须是已路由结果");
        }
        return new AskDataModelAttemptTrace(routed.getModelKey(), routed.getRoutePolicy(), routed.getRouteReason(),
                state, failureCode, switchReason, actualModelKey, usage);
    }

    private static String normalize(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
