package com.xiongx9527.fw.plus.ai.base.model;

/**
 * 模型校验结果：{@code validateModel} 的返回值。
 *
 * <p>校验失败不抛异常——配置错误属于可预期结果，调用方（配置页 / 启动自检）需要拿到
 * 可展示的原因，而不是 try/catch。{@link #getMessage()} 为可直接展示的原因，
 * 成功时为 {@code null}。</p>
 */
public final class ModelValidation {

    private final String model;

    private final boolean valid;

    private final String message;

    private final long latencyMillis;

    private ModelValidation(String model, boolean valid, String message, long latencyMillis) {
        this.model = model;
        this.valid = valid;
        this.message = message;
        this.latencyMillis = latencyMillis;
    }

    public static ModelValidation valid(String model, long latencyMillis) {
        return new ModelValidation(model, true, null, latencyMillis);
    }

    public static ModelValidation invalid(String model, String message, long latencyMillis) {
        return new ModelValidation(model, false, message, latencyMillis);
    }

    public String getModel() {
        return model;
    }

    public boolean isValid() {
        return valid;
    }

    public String getMessage() {
        return message;
    }

    /** 校验耗时（毫秒），便于在配置页展示连通性。 */
    public long getLatencyMillis() {
        return latencyMillis;
    }

    @Override
    public String toString() {
        return "ModelValidation{model=" + model + ", valid=" + valid
                + (valid ? "" : ", message=" + message) + ", latencyMillis=" + latencyMillis + "}";
    }
}
