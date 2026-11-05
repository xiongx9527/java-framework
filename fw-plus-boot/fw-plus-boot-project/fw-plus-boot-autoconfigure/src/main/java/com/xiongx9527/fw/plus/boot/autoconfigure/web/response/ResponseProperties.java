package com.xiongx9527.fw.plus.boot.autoconfigure.web.response;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 统一响应信封与全局异常处理配置（{@code fw.plus.web.response.*}）。
 *
 * <p>默认关闭：只有显式 {@code enabled=true} 的产品才注册信封切面与异常处理器，
 * 避免改变既有产品的响应契约。</p>
 *
 * <h3>典型配置</h3>
 * <pre>
 * fw:
 *   plus:
 *     web:
 *       response:
 *         enabled: true
 *         base-packages: com.example.app
 *         message-key: msg          # 用 msg 替代 message
 *         success-key: ""           # 置空表示不输出 success 字段
 * </pre>
 */
@ConfigurationProperties(prefix = ResponseProperties.PREFIX)
public class ResponseProperties {

    public static final String PREFIX = "fw.plus.web.response";

    /** 是否启用统一响应信封与全局异常处理。 */
    private boolean enabled = false;

    /** 只处理这些包下的 Controller；为空表示全部。 */
    private String[] basePackages = new String[0];

    /** 状态码字段名。 */
    private String codeKey = "code";

    /** 成功标志字段名；置空表示不输出该字段。 */
    private String successKey = "success";

    /** 消息字段名。 */
    private String messageKey = "message";

    /** 数据字段名。 */
    private String dataKey = "data";

    /** 成功状态码。 */
    private int successCode = 0;

    /** 成功提示文案；为 null 时输出 null（前端只读 data）。 */
    private String successMessage;

    /** 是否按状态码前两位推导 HTTP 状态（40→400，50→500）。 */
    private boolean httpStatusFromCode = true;

    /**
     * Reactive 栈信封可缓冲的最大响应体字节数（缺省 1MB）。
     * 超过该值的响应原样透传，避免把大结果集整个读进内存。
     */
    private int maxBufferBytes = 1024 * 1024;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String[] getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(String[] basePackages) {
        this.basePackages = basePackages;
    }

    public String getCodeKey() {
        return codeKey;
    }

    public void setCodeKey(String codeKey) {
        this.codeKey = codeKey;
    }

    public String getSuccessKey() {
        return successKey;
    }

    public void setSuccessKey(String successKey) {
        this.successKey = successKey;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getDataKey() {
        return dataKey;
    }

    public void setDataKey(String dataKey) {
        this.dataKey = dataKey;
    }

    public int getSuccessCode() {
        return successCode;
    }

    public void setSuccessCode(int successCode) {
        this.successCode = successCode;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    public boolean isHttpStatusFromCode() {
        return httpStatusFromCode;
    }

    public void setHttpStatusFromCode(boolean httpStatusFromCode) {
        this.httpStatusFromCode = httpStatusFromCode;
    }

    public int getMaxBufferBytes() {
        return maxBufferBytes;
    }

    public void setMaxBufferBytes(int maxBufferBytes) {
        this.maxBufferBytes = maxBufferBytes;
    }
}
