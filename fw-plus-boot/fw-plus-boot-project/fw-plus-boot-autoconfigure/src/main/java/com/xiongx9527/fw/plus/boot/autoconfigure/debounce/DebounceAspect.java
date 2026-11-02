package com.xiongx9527.fw.plus.boot.autoconfigure.debounce;

import com.xiongx9527.fw.plus.common.debounce.Debounce;
import com.xiongx9527.fw.plus.common.debounce.DebounceBlockedException;
import com.xiongx9527.fw.plus.common.debounce.DebounceKeyBuilder;
import com.xiongx9527.fw.plus.common.debounce.DebounceProperties;
import com.xiongx9527.fw.plus.common.debounce.DebounceUserResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import java.lang.reflect.Method;

/**
 * 接口防抖切面：拦截标注 {@link Debounce} 的方法，按注解维度构造键并交给
 * {@link RequestDebouncer} 原子判定；窗口内重复请求抛 {@link DebounceBlockedException}。
 *
 * <p>用 AOP 而非路径过滤器：可直接拿到方法参数（按接口参数防抖），且能在
 * DispatcherServlet 内通过 {@link DebounceUserResolver} 取得业务侧当前用户上下文。</p>
 */
@Aspect
@Order(50)
public class DebounceAspect {

    private static final Logger log = LoggerFactory.getLogger(DebounceAspect.class);

    private final RequestDebouncer debouncer;
    private final DebounceProperties properties;
    private final DebounceKeyBuilder keyBuilder;
    private final DebounceUserResolver userResolver;

    public DebounceAspect(RequestDebouncer debouncer, DebounceProperties properties,
                          DebounceKeyBuilder keyBuilder, DebounceUserResolver userResolver) {
        this.debouncer = debouncer;
        this.properties = properties;
        this.keyBuilder = keyBuilder;
        this.userResolver = userResolver;
    }

    @Around("@annotation(debounce)")
    public Object around(ProceedingJoinPoint pjp, Debounce debounce) throws Throwable {
        if (!properties.isEnabled()) {
            return pjp.proceed();
        }
        MethodSignature ms = (MethodSignature) pjp.getSignature();
        Method method = ms.getMethod();
        String uid = resolveUid();
        String key = keyBuilder.buildKey(method, pjp.getArgs(), debounce, uid);
        long windowMs = effectiveWindow(debounce, properties.getWindowMs());
        if (!debouncer.allow(key, windowMs)) {
            throw new DebounceBlockedException(debounce.message(), windowMs);
        }
        return pjp.proceed();
    }

    private String resolveUid() {
        if (userResolver == null) {
            return null;
        }
        try {
            return userResolver.currentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 计算本次生效的防抖窗口（毫秒）。
     * 注解 windowMs &gt; 0 时使用注解值（收敛到上限），否则沿用全局窗口。
     */
    static long effectiveWindow(Debounce debounce, long globalWindowMs) {
        long w = debounce.windowMs();
        if (w <= 0L) {
            return globalWindowMs;
        }
        return Math.min(w, DebounceProperties.MAX_WINDOW_MS);
    }
}
