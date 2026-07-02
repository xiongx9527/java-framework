package com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog;

import cn.hutool.core.util.RandomUtil;
import com.xiongx9527.fw.plus.boot.autoconfigure.trace.opentelemetry.OpentelemetryTrace;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.filter.WebCommonFilter;
import com.xiongx9527.fw.plus.boot.common.web.ICommonFilterService;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.web.WebRequest;
import com.xiongx9527.fw.plus.common.web.WebResponse;
import com.plumelog.core.LogMessageThreadLocal;
import com.plumelog.core.TraceId;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class PlumelogFilterService extends WebCommonFilter.DefaultCommonFilterService {

    /**
     * 跟踪ID参数名称常量
     * 用于在请求参数中标识分布式跟踪系统的跟踪ID
     */
    private static final String TRACE_ID_PARAM_NAME = "traceId";

    /**
     * 设置请求的traceId，用于链路追踪
     * 该方法会尝试从OpenTelemetry获取traceId，如果获取失败或为空，则尝试从请求参数中获取，
     * 如果都没有则生成一个随机的traceId
     *
     * @param request Web请求对象，用于获取请求参数
     */
    private void setTraceId(WebRequest request) {
        String traceId = null;
        try {
            traceId = SpringUtils.getBean(OpentelemetryTrace.class).getTraceId();
        } catch (Exception e) {
        }
        if (StringUtils.isNotBlank(traceId)) {
            TraceId.logTraceID.set(traceId);
        } else {
            // 从请求参数中获取traceId
            String[] traceIds = request.getParameterValues(TRACE_ID_PARAM_NAME);
            if (traceIds != null && traceIds.length > 0) {
                traceId = traceIds[0];
            }
            if (StringUtils.isBlank(traceId)) {
                traceId = request.getHeader(TRACE_ID_PARAM_NAME);
            }
            if (StringUtils.isNotBlank(traceId)) {
                TraceId.logTraceID.set(traceId);
            } else {
                TraceId.logTraceID.set(RandomUtil.randomString(32));
            }
        }
    }

    /**
     * 执行过滤器前置处理
     * 该方法在请求处理之前执行，负责添加追踪ID相关的处理逻辑
     *
     * @param request  Web请求对象，包含请求的相关信息
     * @param response Web响应对象，用于构建响应信息
     */
    public void doFilterBefore(WebRequest request, WebResponse response) {
        try {
            setTraceId(request);
        } catch (Exception e) {
        }
        super.doFilterBefore(request, response);
    }

    /**
     * 执行过滤器后置处理
     * 该方法在请求处理之后执行，负责处理追踪消息的结束逻辑、清除线程本地变量中的TraceId和TraceMessage
     */
    public void doFilterAfter() {
        try {
            super.doFilterAfter();
        } catch (Exception e) {
        }
        TraceId.logTraceID.remove();
        LogMessageThreadLocal.logMessageThreadLocal.remove();
    }

}
