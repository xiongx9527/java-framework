package com.xiongx9527.fw.plus.boot.autoconfigure.web.response;

import com.xiongx9527.fw.plus.common.exception.BizException;
import com.xiongx9527.fw.plus.common.model.JsonResult;
import org.springframework.util.StringUtils;

/**
 * 按 {@link ResponseProperties} 构造统一响应体。
 *
 * <p>{@link JsonResult} 默认会写入 {@code code/success/message} 三个键；本类负责把它们
 * 重命名成产品约定的字段名，并在 {@code success-key} 为空时彻底移除该键，
 * 以便与既有前端契约（如 {@code {code,data,msg}}）逐字对齐。</p>
 */
public class ResponseEnvelopeFactory {

    private final ResponseProperties properties;

    public ResponseEnvelopeFactory(ResponseProperties properties) {
        this.properties = properties;
    }

    /** 成功响应：{@code code=successCode}，{@code data} 为原始返回值。 */
    public JsonResult success(Object data) {
        JsonResult result = newResult();
        result.code(properties.getSuccessCode());
        result.data(data);
        result.message(properties.getSuccessMessage());
        return result;
    }

    /** 失败响应：显式指定状态码与消息。 */
    public JsonResult failure(int code, String message, Object data) {
        JsonResult result = newResult();
        result.code(code);
        result.message(message);
        if (data != null) {
            result.data(data);
        } else {
            result.data(null);
        }
        return result;
    }

    /** 失败响应：状态码与附加数据取自 {@link BizException}。 */
    public JsonResult failure(BizException exception) {
        return failure(exception.getCode(), exception.getMessage(), exception.getData());
    }

    /** 是否已是统一信封结构（避免二次包装）。 */
    public boolean isWrapped(Object body) {
        if (!(body instanceof java.util.Map)) {
            return false;
        }
        java.util.Map<?, ?> map = (java.util.Map<?, ?>) body;
        return map.containsKey(properties.getCodeKey()) && map.containsKey(properties.getDataKey());
    }

    /** 由 8 位状态码推导 HTTP 状态：40 开头 → 400，50 开头 → 500，其余 200。 */
    public int httpStatus(int code) {
        if (!properties.isHttpStatusFromCode()) {
            return 200;
        }
        int source = code / 1_000_000;
        if (source == 40) {
            return 400;
        }
        if (source == 50) {
            return 500;
        }
        return 200;
    }

    private JsonResult newResult() {
        JsonResult result = new JsonResult();
        result.renameCode(properties.getCodeKey());
        result.renameMessage(properties.getMessageKey());
        result.renameData(properties.getDataKey());
        if (StringUtils.hasText(properties.getSuccessKey())) {
            result.renameSuccess(properties.getSuccessKey());
        } else {
            result.remove("success");
        }
        return result;
    }
}
