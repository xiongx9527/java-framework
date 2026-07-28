package org.ssssssss.magicapi.core.model;

import com.xiongx9527.fw.plus.common.model.JsonResult;
import org.apache.commons.lang3.StringUtils;

/**
 * Magic-API 标准响应体，继承 {@link JsonResult}，额外携带 timestamp 和 executeTime。
 *
 * <p>与父类的区别：构造时默认 code=1（而非 0），且自动填充 timestamp。</p>
 * <p>基础字段（code / success / message / data / traceId / spanId）由父类 HashMap 存储。仅 timestamp / executeTime 为本类特有。</p>
 */
public class JsonBean<T> extends JsonResult {

	private String keyTimestamp = "timestamp";         // 默认 "code"
	private String keyExecuteTime = "executeTime";   // 默认 "success"

    // ═══════════════════ 构造 — code 默认 1（Magic-API 约定） ═══════════════════

    public JsonBean() {
        setTimestamp(System.currentTimeMillis());
        code(1);
    }

    public JsonBean(int code, String message) {
        this();
        code(code).message(message);
    }

    public JsonBean(int code, String message, T data) {
        this(code, message);
        data(data);
    }

    public JsonBean(int code, String message, T data, Integer executeTime) {
        this(code, message, data);
        setExecuteTime(executeTime);
    }

    public JsonBean(JsonCode jsonCode) {
        this(jsonCode.getCode(), jsonCode.getMessage());
    }

    public JsonBean(JsonCode jsonCode, T data) {
        this(jsonCode.getCode(), jsonCode.getMessage(), data);
    }

    public JsonBean(T data) {
        this();
        data(data);
    }

	public JsonResult renameTimestamp(String newKey)    {
		if (StringUtils.isBlank(newKey)) return this;
		if (keyTimestamp.equals(newKey)) return this;
		JsonResult r = rename(keyTimestamp, newKey);
		keyTimestamp = newKey;
		return r;
	}
	public JsonResult renameExecuteTime(String newKey) {
		if (StringUtils.isBlank(newKey)) return this;
		if (keyExecuteTime.equals(newKey)) return this;
		JsonResult r = rename(keyExecuteTime, newKey);
		keyExecuteTime = newKey;
		return r;
	}

    // ═══════════════════ 特有 getter/setter ═══════════════════

    public long getTimestamp() { return (long) get(keyTimestamp); }
    public void setTimestamp(long timestamp) { put(keyTimestamp, timestamp); }

    public Integer getExecuteTime() { return (Integer) get(keyExecuteTime); }
    public void setExecuteTime(Integer executeTime) { put(keyExecuteTime, executeTime); }

}
