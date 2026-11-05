package com.xiongx9527.fw.plus.boot.autoconfigure.web.response;

import com.xiongx9527.fw.plus.common.exception.BizException;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.common.model.JsonResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.ArrayList;
import java.util.List;

/**
 * WebFlux 栈的全局异常处理，语义与 Servlet 版 {@link GlobalExceptionHandler} 对齐。
 *
 * <p>WebFlux 没有 {@code MissingServletRequestParameterException} 等 Servlet 异常，
 * 参数问题统一由 {@link ServerWebInputException} / {@link WebExchangeBindException} 表达，
 * 因此单独成类，避免在 Servlet 缺失或 WebFlux 缺失时出现类加载失败。</p>
 */
@RestControllerAdvice
public class ReactiveGlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ReactiveGlobalExceptionHandler.class);

    private static final int CODE_PARAM_INVALID = 40_001_100;
    private static final int CODE_BODY_UNREADABLE = 40_001_110;
    private static final int CODE_INTERNAL_ERROR = 50_010_100;

    private final ResponseEnvelopeFactory factory;

    public ReactiveGlobalExceptionHandler(ResponseEnvelopeFactory factory) {
        this.factory = factory;
    }

    /** 业务异常：状态码、文案、附加数据全部沿用异常自身。 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<JsonResult> handleBiz(BizException ex) {
        log.warn("biz exception: code={}, message={}", ex.getCode(), ex.getMessage());
        return ResponseEntity.status(factory.httpStatus(ex.getCode()))
                .body(factory.failure(ex.getCode(), ex.getMessage(), ex.getData()));
    }

    /** 表单/查询参数绑定与 {@code @Valid} 校验失败。 */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<JsonResult> handleBind(WebExchangeBindException ex) {
        List<String> messages = new ArrayList<String>();
        for (FieldError error : ex.getFieldErrors()) {
            messages.add(error.getField() + " " + error.getDefaultMessage());
        }
        String message = messages.isEmpty() ? I18nMessageResolver.resolve(CODE_PARAM_INVALID) : String.join("; ", messages);
        log.warn("reactive validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(factory.failure(CODE_PARAM_INVALID, message, null));
    }

    /** 请求体/参数无法解析。 */
    @ExceptionHandler(ServerWebInputException.class)
    public ResponseEntity<JsonResult> handleInput(ServerWebInputException ex) {
        log.warn("reactive request input invalid: {}", ex.getReason());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(factory.failure(CODE_BODY_UNREADABLE, I18nMessageResolver.resolve(CODE_BODY_UNREADABLE), null));
    }

    /** 显式抛出带 HTTP 状态的路由异常（404/405 等）。 */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<JsonResult> handleStatus(ResponseStatusException ex) {
        HttpStatus status = ex.getStatus();
        int code = status.is4xxClientError() ? 40_004_102 : CODE_INTERNAL_ERROR;
        String reason = ex.getReason() == null ? status.getReasonPhrase() : ex.getReason();
        return ResponseEntity.status(status).body(factory.failure(code, reason, null));
    }

    /** 兜底：记录完整堆栈，对外只给通用文案。 */
    @ExceptionHandler(Throwable.class)
    public ResponseEntity<JsonResult> handleThrowable(Throwable ex) {
        log.error("unhandled reactive exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(factory.failure(CODE_INTERNAL_ERROR, I18nMessageResolver.resolve(CODE_INTERNAL_ERROR), null));
    }
}
