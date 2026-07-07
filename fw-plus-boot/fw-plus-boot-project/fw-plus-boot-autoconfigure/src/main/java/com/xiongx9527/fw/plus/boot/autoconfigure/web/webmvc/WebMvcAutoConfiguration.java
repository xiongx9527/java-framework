package com.xiongx9527.fw.plus.boot.autoconfigure.web.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.properties.WebProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.webmvc.filter.WebMvcCommonFilter;
import com.xiongx9527.fw.plus.boot.common.CommonAutoConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.support.ServletRequestHandledEvent;

import java.util.Map;

@Slf4j
@Configuration
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
@Import({WebMvcCommonFilter.class})
public class WebMvcAutoConfiguration extends CommonAutoConfiguration implements ApplicationListener<ServletRequestHandledEvent> {

    private final WebProperties webProperties;

    public WebMvcAutoConfiguration(WebProperties webProperties) {
        this.webProperties = webProperties;
    }

    /**
     * 处理阈值事件，根据处理时间与阈值的比较结果进行相应的日志记录和告警发送
     *
     * @param event Servlet请求处理事件对象，包含请求的相关信息
     * @param processingTime 请求的实际处理时间（毫秒）
     * @param level 阈值配置项，键为阈值时间（毫秒），值为告警级别名称
     */
    private void handleThresholdEvent(ServletRequestHandledEvent event, long processingTime, Map.Entry<Long, String> level) {
        String levelName = level.getValue();
        long threshold = level.getKey();

        switch (levelName) {
            case "WARN":
                logWarnLevel(event, processingTime, threshold);
                break;
            case "ERROR":
                logErrorLevel(event, processingTime, threshold);
                break;
            case "CRITICAL":
                logCriticalLevel(event, processingTime, threshold);
                break;
        }

        // 发送告警  todo
        //sendAlert(event, processingTime, levelName, threshold);
    }

    /**
     * 记录慢请求警告日志
     * 当请求处理时间超过阈值时，输出警告级别的日志信息
     *
     * @param event Servlet请求处理事件对象，包含请求的基本信息
     * @param processingTime 请求实际处理耗时（毫秒）
     * @param threshold 慢请求时间阈值（毫秒）
     */
    private void logWarnLevel(ServletRequestHandledEvent event, long processingTime, long threshold) {
        if (log.isWarnEnabled()) {
            log.warn("[慢请求-WARN] {} {} - 耗时: {}ms (阈值: {}ms)",
                    event.getMethod(), event.getRequestUrl(), processingTime, threshold);
        }
    }

    /**
     * 记录慢请求错误日志
     * 当请求处理时间超过阈值时，记录详细的错误日志信息
     *
     * @param event Servlet请求处理事件对象，包含请求相关信息
     * @param processingTime 实际处理耗时(毫秒)
     * @param threshold 配置的耗时阈值(毫秒)
     */
    private void logErrorLevel(ServletRequestHandledEvent event, long processingTime, long threshold) {
        log.error("[慢请求-ERROR] 请求: {} {} 客户端: {} 耗时: {}ms (超过阈值: {}ms) 建议: 检查数据库查询或外部服务调用",
                event.getMethod(), event.getRequestUrl(),
                event.getClientAddress(), processingTime, threshold
        );
    }

    /**
     * 记录关键级别的慢请求日志
     * 当请求处理时间严重超过阈值时，记录错误级别的日志并触发告警
     *
     * @param event Servlet请求处理事件对象，包含请求的详细信息
     * @param processingTime 请求实际处理耗时（毫秒）
     * @param threshold 慢请求时间阈值（毫秒）
     */
    private void logCriticalLevel(ServletRequestHandledEvent event, long processingTime, long threshold) {
        log.error("[慢请求-CRITICAL] 请求: {} {} 客户端: {} 耗时: {}ms (严重超过阈值: {}ms) 是否异常: {} 紧急: 需要立即检查系统性能",
                event.getMethod(), event.getRequestUrl(),
                event.getClientAddress(), processingTime, threshold,
                event.getFailureCause() != null ? "是" : "否"
        );
    }

    /**
     * 单独打印请求处理异常日志
     * 当请求处理失败时，记录详细的异常信息
     *
     * @param event Servlet请求处理事件对象，包含请求相关信息和异常信息
     * @param processingTime 实际处理耗时(毫秒)
     */
    private void logExceptionDetails(ServletRequestHandledEvent event, long processingTime) {
        Throwable failureCause = event.getFailureCause();
        if (failureCause != null) {
            log.error("[异常请求] 请求: {} {} 客户端: {} 耗时: {}ms 异常类型: {} 异常信息: {}",
                    event.getMethod(), event.getRequestUrl(),
                    event.getClientAddress(), processingTime,
                    getExceptionType(failureCause),
                    failureCause.getMessage(),
                    failureCause);
        }
    }

    String getExceptionType(Throwable failureCause) {
        return failureCause.getClass().getName();
    }

    /**
     * 处理Servlet请求处理完成事件的方法
     *
     * @param event Servlet请求处理事件对象，包含请求处理的相关信息
     */
    @Override
    public void onApplicationEvent(ServletRequestHandledEvent event) {
        if (!this.webProperties.isSlowRequestEnabled()) {
            return;
        }
        long processingTime = event.getProcessingTimeMillis();
        Map.Entry<Long, String> matchedLevel = null;
        Map<Long, String> thresholdLevels = this.webProperties.getSlowRequest().getThresholdLevels();
        if (!CollectionUtils.isEmpty(thresholdLevels)) {
            matchedLevel = thresholdLevels.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .filter(entry -> processingTime >= entry.getKey())
                    .reduce((first, second) -> second)
                    .orElse(null);
        }

        if (this.webProperties.getSlowRequest().isPrintExceptionDetails() && event.getFailureCause() != null) {
            logExceptionDetails(event, processingTime);
        }

        if (matchedLevel != null) {
            handleThresholdEvent(event, processingTime, matchedLevel);
        }
    }

}
