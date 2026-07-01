package com.xiongx9527.fw.plus.common.model;

import com.xiongx9527.fw.plus.common.exception.BizException;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;

/**
 * 通用 JSON 响应体，继承 {@link HashMap}，可直接被 Jackson / fastjson2 序列化。
 *
 * <p>内置 6 个基础 key（code / success / message / data / traceId / spanId），
 * 均可通过构造参数或 {@code rename*} 方法动态替换为任意名称。
 * 需要额外字段时，直接调用 {@link #value(String, Object)} 或 {@link #put(String, Object)} 自由扩展。</p>
 *
 * <h3>典型用法</h3>
 * <pre>{@code
 *   // ① 默认 —— 6 个基础 key 用标准名
 *   new JsonResult();
 *   // → {"code":0, "success":true, "message":"success"}
 *
 *   // ② 设值 —— 所有快捷方法均返回 this，支持链式调用
 *   new JsonResult().code(1).success(false).message("失败").data(orderList);
 *   // → {"code":1, "success":false, "message":"失败", "data":[...]}
 *
 *   // ③ 换 key —— 用 msg 替代 message
 *   new JsonResult().renameMessage("msg").message("ok");
 *   // → {"code":0, "success":true, "msg":"ok"}
 *
 *   // ④ 构造时指定自定义 key
 *   new JsonResult("status", "ok", "info", "payload", "tid", "sid");
 *
 *   // ⑤ 静态工厂 —— 常用组合
 *   JsonResult.ok(userList);
 *   JsonResult.fail(BizException.param(1, "参数缺失"));
 *   JsonResult.msgStyle().data(orderList);
 *
 *   // ⑥ 自由扩展 —— 任何 key-value 都能链式追加
 *   new JsonResult().value("total", 100).value("rows", list).put("extra", obj);
 * }</pre>
 *
 * <h3>6 个可重命名的基础 key 速查</h3>
 * <pre>
 *   字段      默认 key      设值方法              取值方法               重命名方法
 *   ─────────────────────────────────────────────────────────────────────────
 *   状态码    code         {@link #code(int)}      {@link #getCode()}      {@link #renameCode(String)}
 *   成功标志  success      {@link #success(boolean)} {@link #isSuccess()}   {@link #renameSuccess(String)}
 *   消息文本  message      {@link #message(String)} {@link #getMessage()}   {@link #renameMessage(String)}
 *   返回数据  data         {@link #data(Object)}   {@link #getData()}      {@link #renameData(String)}
 *   链路ID    traceId      {@link #traceId(String)} {@link #getTraceId()}   {@link #renameTraceId(String)}
 *   SpanID    spanId       {@link #spanId(String)}  {@link #getSpanId()}    {@link #renameSpanId(String)}
 * </pre>
 */
public class JsonResult<T> extends HashMap<String, Object> {

    private static final long serialVersionUID = 1L;

    /** 6 个基础 key 的当前名称。构造时赋值，运行时可调用 rename* 方法更换 */
    private String keyCode = "code";         // 默认 "code"
    private String keySuccess = "success";   // 默认 "success"
    private String keyMessage = "message";   // 默认 "message"
    private String keyData = "data";         // 默认 "data"
    private String keyTraceId = "traceId";   // 默认 "traceId"
    private String keySpanId = "spanId";     // 默认 "spanId"

    // ═══════════════════ 构造 ═══════════════════

    /** 使用默认 key */
    public JsonResult() {
        this(null, null, null, null, null, null);
    }

    /** 快捷构造：只设 message，其余用默认 */
    public JsonResult(String message) {
        this();
        message(message);
    }

    /** 构造时指定 code + message 默认值 */
    public JsonResult(int code, String message) {
        this();
        code(code).message(message);
    }

    /**
     * 自定义 6 个基础 key + 默认值。传 null/空则使用默认。
     * <pre>{@code
     *   new JsonResult(null, null, "msg", null, null, null)              // key 定制
     *   new JsonResult(null, null, null, null, null, null, 1, "ok")         // key + 默认值
     * }</pre>
     */
    public JsonResult(String codeKey, String successKey, String messageKey, String dataKey,
                      String traceIdKey, String spanIdKey, int code, String message) {
        this(codeKey, successKey, messageKey, dataKey, traceIdKey, spanIdKey);
        code(code).message(message);
    }

    /**
     * 自定义 6 个基础 key，传 null/空则使用默认值。
     * <pre>{@code new JsonResult(null, null, "msg", null, null, null)  // 仅替换 message → msg}</pre>
     */
    public JsonResult(String codeKey, String successKey, String messageKey, String dataKey,
                      String traceIdKey, String spanIdKey) {
        this.keyCode    = StringUtils.isBlank(codeKey)    ? this.keyCode    : codeKey;
        this.keySuccess = StringUtils.isBlank(successKey) ? this.keySuccess : successKey;
        this.keyMessage = StringUtils.isBlank(messageKey) ? this.keyMessage : messageKey;
        this.keyData    = StringUtils.isBlank(dataKey)    ? this.keyData    : dataKey;
        this.keyTraceId = StringUtils.isBlank(traceIdKey) ? this.keyTraceId : traceIdKey;
        this.keySpanId  = StringUtils.isBlank(spanIdKey)  ? this.keySpanId  : spanIdKey;
        initDefaults();
    }

    // ═══════════════════ 常用静态工厂 ═══════════════════

    /** 成功（无 data）：{@code {"code":0, "success":true, "message":"success"}} */
    public static JsonResult ok() { return new JsonResult(); }

    /** 成功 + 自定义 message */
    public static JsonResult ok(String message) { return new JsonResult().message(message); }

    /** 成功 + 自定义 message + data */
    public static JsonResult ok(String message, Object data) { return new JsonResult().message(message).data(data); }

    /**
     * 失败响应 — 从 {@link BizException} 提取 code，可覆盖 message / data。
     * <pre>{@code
     *   JsonResult.fail(e);                              // 直接用异常的 message + data
     *   JsonResult.fail(e, "用户名已存在");                // 覆盖 message
     *   JsonResult.fail(e, "库存不足", Map.of("sku","A")); // 覆盖 message + data
     * }</pre>
     */
    public static JsonResult fail(BizException e) {
        return fail(e, e.getMessage(), e.getData());
    }

    /** 失败 + 覆盖 message。code 和 data 仍取自 {@link BizException} */
    public static JsonResult fail(BizException e, String message) {
        return fail(e, message, e.getData());
    }

    /**
     * 失败 + 覆盖 message 和 data。code 仍取自 {@link BizException}。
     * @param e        异常对象，提取 8 位状态码
     * @param message  覆盖的错误消息
     * @param data     覆盖的附加数据，传 null 则不输出
     */
    public static JsonResult fail(BizException e, String message, Object data) {
        JsonResult r = new JsonResult().code(e.getCode()).success(false).message(message);
        if (data != null) r.data(data);
        return r;
    }

    /** msg 风格：用 msg 替代 message 作为提示 key */
    public static JsonResult msgStyle() { return new JsonResult().renameMessage("msg"); }

    // ═══════════════════ 内部 ═══════════════════

    /** 写入默认值：code=0, success=true, message="success" */
    private void initDefaults() {
        value(keyCode, 0);
        value(keySuccess, true);
        value(keyMessage, "success");
    }

    // ═══════════════════ 动态重命名 — 迁移旧值到新 key ═══════════════════

    public JsonResult renameCode(String newKey)    { return rename(keyCode, newKey); }
    public JsonResult renameSuccess(String newKey) { return rename(keySuccess, newKey); }
    public JsonResult renameMessage(String newKey) { return rename(keyMessage, newKey); }
    public JsonResult renameData(String newKey)    { return rename(keyData, newKey); }
    public JsonResult renameTraceId(String newKey) { return rename(keyTraceId, newKey); }
    public JsonResult renameSpanId(String newKey)  { return rename(keySpanId, newKey); }

    /** 将任意 key 的值迁移到新 key。如果是 6 个基础 key 之一，同步更新实例字段 */
    public JsonResult rename(String oldKey, String newKey) {
        if (StringUtils.isBlank(newKey)) return this;
        if (oldKey.equals(newKey)) return this;
        if (containsKey(oldKey)) {
            put(newKey, remove(oldKey));
        }
        if (oldKey.equals(keyCode))    keyCode    = newKey;
        if (oldKey.equals(keySuccess)) keySuccess = newKey;
        if (oldKey.equals(keyMessage)) keyMessage = newKey;
        if (oldKey.equals(keyData))    keyData    = newKey;
        if (oldKey.equals(keyTraceId)) keyTraceId = newKey;
        if (oldKey.equals(keySpanId))  keySpanId  = newKey;
        return this;
    }

    // ═══════════════════ 快捷设值 — 全部委托给 {@link #value(String, Object)} ═══════════════════

    /** 设状态码，默认 key={@code keyCode} */
    public JsonResult code(int code)       { return value(keyCode, code); }
    /** 设成功标志，默认 key={@code keySuccess} */
    public JsonResult success(boolean s)   { return value(keySuccess, s); }
    /** 设消息文本，默认 key={@code keyMessage} */
    public JsonResult message(String msg)  { return value(keyMessage, msg); }
    /** 设返回数据，默认 key={@code keyData} */
    public JsonResult data(T data)    { return value(keyData, data); }
    /** 设链路追踪 ID，默认 key={@code keyTraceId} */
    public JsonResult traceId(String traceId)   { return value(keyTraceId, traceId); }
    /** 设请求 SpanID，默认 key={@code keySpanId} */
    public JsonResult spanId(String spanId)    { return value(keySpanId, spanId); }

    /** 设状态码，默认 key={@code keyCode} */
    public void setCode(int code)       { value(keyCode, code); }
    /** 设成功标志，默认 key={@code keySuccess} */
    public void setSuccess(boolean s)   { value(keySuccess, s); }
    /** 设消息文本，默认 key={@code keyMessage} */
    public void setMessage(String msg) { value(keyMessage, msg); }
    /** 设返回数据，默认 key={@code keyData} */
    public void setData(T data)    { value(keyData, data); }
    /** 设链路追踪 ID，默认 key={@code keyTraceId} */
    public void setTraceId(String traceId)   { value(keyTraceId, traceId); }
    /** 设请求 SpanID，默认 key={@code keySpanId} */
    public void setSpanId(String spanId)    { value(keySpanId, spanId); }

    /**
     * 写入任意 key-value，链式调用。
     * <p>所有快捷设值方法（code / success / message / data / traceId / spanId）均委托至此，
     * 子类可覆写此方法实现统一的序列化控制。</p>
     */
    public JsonResult value(String key, Object val) { put(key, val); return this; }

    // ═══════════════════ 快捷取值 ═══════════════════

    /** 取状态码，不存在返回 0 */
    public int getCode()            { return (int) getOrDefault(keyCode, 0); }
    /** 取成功标志，不存在返回 true */
    public boolean isSuccess()      { return (boolean) getOrDefault(keySuccess, true); }
    /** 取消息文本，不存在返回 "success" */
    public String getMessage()      { return (String) getOrDefault(keyMessage, "success"); }
    /** 取返回数据，不存在返回 null */
    public T getData()         { return (T) get(keyData); }
    /** 取链路追踪 ID，不存在返回 null */
    public String getTraceId()      { return (String) get(keyTraceId); }
    /** 取请求 SpanID，不存在返回 null */
    public String getSpanId()       { return (String) get(keySpanId); }
}
