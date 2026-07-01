package com.xx9527.fw.plus.boot.autoconfigure.web.webflux;

import com.xx9527.fw.plus.boot.autoconfigure.web.properties.WebProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;


@Slf4j
public class WebFluxRequestMonitorHandler implements WebFilter, Ordered {

    /**
     * Web属性配置对象
     * 用于存储和管理Web相关的配置属性
     */
    private final WebProperties webProperties;

    /**
     * 构造函数，初始化WebFlux请求监控处理器
     * @param webProperties Web配置属性对象
     */
    public WebFluxRequestMonitorHandler(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    /**
     * 过滤器方法，用于监控请求处理时间
     * @param exchange 服务器Web交换对象
     * @param chain Web过滤器链
     * @return Mono<Void> 异步处理结果
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startTime = System.currentTimeMillis();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        return chain.filter(exchange)
                .doOnError(errorRef::set)
                .doFinally(signalType -> {
                    long processingTime = System.currentTimeMillis() - startTime;
                    handleRequestCompletion(exchange, processingTime,
                            signalType == SignalType.ON_ERROR ? errorRef.get() : null);
                });
    }

    /**
     * 处理请求完成逻辑，检查处理时间是否超过阈值并记录日志
     * @param exchange 服务器Web交换对象
     * @param processingTime 请求处理时间（毫秒）
     * @param throwable 异常对象，如果请求正常完成则为null
     */
    private void handleRequestCompletion(ServerWebExchange exchange, long processingTime, Throwable throwable) {
        if (!webProperties.isSlowRequestEnabled()) {
            return;
        }

        // 检查处理时间是否超过阈值（按 key 升序排列，取最高匹配阈值）
        webProperties.getSlowRequest().getThresholdLevels()
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .filter(entry -> processingTime >= entry.getKey())
                .reduce((first, second) -> second)
                .ifPresent(entry -> handleThresholdEvent(exchange, processingTime, entry));

        if (throwable != null && webProperties.getSlowRequest().isPrintExceptionDetails()) {
            logExceptionDetails(exchange, processingTime, throwable);
        }
    }

    /**
     * 根据阈值级别处理相应的日志记录事件
     * @param exchange 服务器Web交换对象
     * @param processingTime 请求处理时间（毫秒）
     * @param level 阈值级别配置项
     */
    private void handleThresholdEvent(ServerWebExchange exchange, long processingTime, Map.Entry<Long, String> level) {
        String levelName = level.getValue();
        long threshold = level.getKey();

        switch (levelName) {
            case "WARN":
                logWarnLevel(exchange, processingTime, threshold);
                break;
            case "ERROR":
                logErrorLevel(exchange, processingTime, threshold);
                break;
            case "CRITICAL":
                logCriticalLevel(exchange, processingTime, threshold);
                break;
        }
    }

    /**
     * 记录警告级别的慢请求日志
     * @param exchange 服务器Web交换对象
     * @param processingTime 请求处理时间（毫秒）
     * @param threshold 触发警告的阈值时间（毫秒）
     */
    private void logWarnLevel(ServerWebExchange exchange, long processingTime, long threshold) {
        if (log.isWarnEnabled()) {
            log.warn("[慢请求-WARN] {} {} - 耗时: {}ms (阈值: {}ms)",
                    exchange.getRequest().getMethod(),
                    exchange.getRequest().getURI().getPath(),
                    processingTime, threshold);
        }
    }

    /**
     * 记录错误级别的慢请求日志
     * @param exchange 服务器Web交换对象
     * @param processingTime 请求处理时间（毫秒）
     * @param threshold 触发错误的阈值时间（毫秒）
     */
    private void logErrorLevel(ServerWebExchange exchange, long processingTime, long threshold) {
        log.error("[慢请求-ERROR] 请求: {} {} 客户端: {} 耗时: {}ms (超过阈值: {}ms)",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                exchange.getRequest().getRemoteAddress(),
                processingTime, threshold);
    }

    /**
     * 记录严重级别的慢请求日志
     * @param exchange 服务器Web交换对象
     * @param processingTime 请求处理时间（毫秒）
     * @param threshold 触发严重级别的阈值时间（毫秒）
     */
    private void logCriticalLevel(ServerWebExchange exchange, long processingTime, long threshold) {
        log.error("[慢请求-CRITICAL] 请求: {} {} 客户端: {} 耗时: {}ms (严重超过阈值: {}ms)",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                exchange.getRequest().getRemoteAddress(),
                processingTime, threshold);
    }

    /**
     * 记录异常请求的详细信息
     * @param exchange 服务器Web交换对象
     * @param processingTime 请求处理时间（毫秒）
     * @param throwable 异常对象
     */
    private void logExceptionDetails(ServerWebExchange exchange, long processingTime, Throwable throwable) {
        log.error("[异常请求] 请求: {} {} 客户端: {} 耗时: {}ms 异常: {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                exchange.getRequest().getRemoteAddress(),
                processingTime,
                throwable.getMessage(),
                throwable);
    }

    /**
     * 获取过滤器的执行顺序
     * @return 返回过滤器优先级，确保在其他处理器之后执行
     */
    @Override
    public int getOrder() {
        // 设置优先级，确保在其他处理器之后执行
        return Ordered.LOWEST_PRECEDENCE;
    }

}
