package com.xiongx9527.fw.plus.boot.autoconfigure.web.response;

import com.xiongx9527.fw.plus.common.exception.BizException;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.common.model.JsonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常处理：把各类异常统一转换成 {@link JsonResult} 信封，HTTP 状态按状态码推导。
 *
 * <p>错误码沿用框架 8 位结构（{@code 来源2位 + 分类3位 + 编号3位}），此处占用
 * {@code specific} 的 100-109 段，业务自定义码建议从 200 起分配。</p>
 *
 * <p>兜底异常不把堆栈或原始消息透出给调用方（避免泄露内部实现），只在日志中记录；
 * 需要透出时业务侧抛 {@link BizException} 并自带文案。</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 参数校验失败：40(客户端) + 001(请求/参数) + 100 */
    private static final int CODE_PARAM_INVALID = 40_001_100;
    /** 请求体无法解析：40 + 001 + 101 */
    private static final int CODE_BODY_UNREADABLE = 40_001_110;
    /** 缺少请求参数：40 + 001 + 102 */
    private static final int CODE_PARAM_MISSING = 40_001_111;
    /** 参数类型不匹配：40 + 001 + 103 */
    private static final int CODE_PARAM_TYPE_MISMATCH = 40_001_112;
    /** 方法不支持：40 + 004(资源) + 100 */
    private static final int CODE_METHOD_NOT_SUPPORTED = 40_004_100;
    /** 媒体类型不支持：40 + 004 + 101 */
    private static final int CODE_MEDIA_TYPE_NOT_SUPPORTED = 40_004_101;
    /** 资源不存在：40 + 004 + 102 */
    private static final int CODE_NOT_FOUND = 40_004_102;
    /** 服务端未知错误：50 + 010(系统/基础设施) + 100 */
    private static final int CODE_INTERNAL_ERROR = 50_010_100;

    private final ResponseEnvelopeFactory factory;

    public GlobalExceptionHandler(ResponseEnvelopeFactory factory) {
        this.factory = factory;
    }

    /** 业务异常：状态码、文案、附加数据全部沿用异常自身。 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<JsonResult> handleBiz(BizException ex) {
        log.warn("biz exception: code={}, message={}", ex.getCode(), ex.getMessage());
        return build(ex.getCode(), ex.getMessage(), ex.getData());
    }

    /** {@code @Valid} 校验失败（MethodArgumentNotValidException 是 BindException 子类）。 */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<JsonResult> handleBind(BindException ex) {
        List<String> messages = new ArrayList<String>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            messages.add(error.getField() + " " + error.getDefaultMessage());
        }
        String message = messages.isEmpty() ? I18nMessageResolver.resolve(CODE_PARAM_INVALID) : String.join("; ", messages);
        log.warn("validation failed: {}", message);
        return build(CODE_PARAM_INVALID, message, null);
    }

    /** 请求体缺失或 JSON 无法解析。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<JsonResult> handleUnreadable(HttpMessageNotReadableException ex) {
        log.warn("request body unreadable: {}", ex.getMessage());
        return build(CODE_BODY_UNREADABLE, I18nMessageResolver.resolve(CODE_BODY_UNREADABLE), null);
    }

    /** 缺少必填的请求参数。 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<JsonResult> handleMissingParam(MissingServletRequestParameterException ex) {
        return build(CODE_PARAM_MISSING, I18nMessageResolver.resolve(CODE_PARAM_MISSING, ex.getParameterName()), null);
    }

    /** 请求参数类型不匹配。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<JsonResult> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(CODE_PARAM_TYPE_MISMATCH, I18nMessageResolver.resolve(CODE_PARAM_TYPE_MISMATCH, ex.getName()), null);
    }

    /** 请求方法不支持（GET 调 POST 接口等）。 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<JsonResult> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return build(CODE_METHOD_NOT_SUPPORTED, I18nMessageResolver.resolve(CODE_METHOD_NOT_SUPPORTED, ex.getMethod()), null);
    }

    /** Content-Type 不支持。 */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<JsonResult> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        return build(CODE_MEDIA_TYPE_NOT_SUPPORTED, I18nMessageResolver.resolve(CODE_MEDIA_TYPE_NOT_SUPPORTED, String.valueOf(ex.getContentType())), null);
    }

    /** 未匹配到处理器（需开启 throw-exception-if-no-handler-found）。 */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<JsonResult> handleNotFound(NoHandlerFoundException ex) {
        return build(CODE_NOT_FOUND, I18nMessageResolver.resolve(CODE_NOT_FOUND, ex.getRequestURL()), null);
    }

    /** 兜底：记录完整堆栈，对外只给通用文案。 */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<JsonResult> handleThrowable(Throwable ex) {
        log.error("unhandled exception", ex);
        return build(CODE_INTERNAL_ERROR, I18nMessageResolver.resolve(CODE_INTERNAL_ERROR), null);
    }

    private ResponseEntity<JsonResult> build(int code, String message, Object data) {
        JsonResult body = factory.failure(code, message, data);
        return ResponseEntity.status(factory.httpStatus(code)).body(body);
    }
}
