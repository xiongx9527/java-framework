package com.xiongx9527.fw.plus.common.debounce;

import com.xiongx9527.fw.plus.common.enums.ErrorCategory;
import com.xiongx9527.fw.plus.common.enums.ErrorSource;
import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * 防抖拦截异常：窗口内重复请求时由防抖切面抛出，
 * 由业务侧的全局异常处理器统一转为 HTTP 429（统一结果信封 + Retry-After）。
 *
 * <p>继承 {@link BizException}，携带统一状态码 {@link #CODE}（CLIENT + IDEMPOTENCY + 001），
 * 业务侧可复用框架统一的错误码 / i18n 体系；同时保留自定义提示
 * （来自 {@link Debounce#message()}）与本次生效窗口（来自 {@link Debounce#windowMs()}）。
 * 提示为空时回退 i18n 默认文案，窗口 &lt;=0 时由处理器回退全局窗口。</p>
 */
public class DebounceBlockedException extends BizException {

    private static final long serialVersionUID = 1L;

    /** 状态码 40025001（CLIENT/IDEMPOTENCY/001 重复提交）。 */
    public static final int CODE = ErrorSource.CLIENT.getCode() * 1_000_000
            + ErrorCategory.IDEMPOTENCY.getCode() * 1_000 + 1;

    private final String detailOverride;
    private final long windowMs;

    public DebounceBlockedException() {
        this(null, -1L);
    }

    public DebounceBlockedException(String detailOverride) {
        this(detailOverride, -1L);
    }

    /**
     * @param detailOverride 自定义拦截提示（可为空，由处理器回退 i18n 默认文案）
     * @param windowMs       本次生效的防抖窗口（毫秒，&lt;=0 表示由处理器回退全局窗口）
     */
    public DebounceBlockedException(String detailOverride, long windowMs) {
        super(CODE, (String) null);
        this.detailOverride = detailOverride;
        this.windowMs = windowMs;
    }

    /** 自定义拦截提示；为空表示使用默认文案。 */
    public String getDetailOverride() {
        return detailOverride;
    }

    /** 本次生效窗口（毫秒）；&lt;=0 表示未携带，由处理器回退全局窗口。 */
    public long getWindowMs() {
        return windowMs;
    }
}
