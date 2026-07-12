package com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog;

import cn.hutool.core.util.RandomUtil;
import com.xiongx9527.fw.plus.boot.autoconfigure.trace.opentelemetry.OpentelemetryTrace;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.filter.WebCommonFilter;
import com.xiongx9527.fw.plus.common.web.WebRequest;
import com.xiongx9527.fw.plus.common.web.WebResponse;
import com.plumelog.core.LogMessageThreadLocal;
import com.plumelog.core.TraceId;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class PlumelogFilterService extends WebCommonFilter.DefaultCommonFilterService {

    private static final String TRACE_ID_PARAM_NAME = "traceId";

    @Autowired(required = false)
    private OpentelemetryTrace opentelemetryTrace;

    private void setTraceId(WebRequest request) {
        String traceId = null;
        if (opentelemetryTrace != null) {
            try {
                traceId = opentelemetryTrace.getTraceId();
            } catch (Exception e) {
                log.trace("Failed to get traceId from OpenTelemetry", e);
            }
        }
        if (StringUtils.isNotBlank(traceId)) {
            TraceId.logTraceID.set(traceId);
        } else {
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

    @Override
    public void doFilterBefore(WebRequest request, WebResponse response) {
        try {
            setTraceId(request);
        } catch (Exception e) {
            log.trace("Failed to set traceId in filter", e);
        }
        super.doFilterBefore(request, response);
    }

    @Override
    public void doFilterAfter() {
        try {
            super.doFilterAfter();
        } catch (Exception e) {
            log.warn("doFilterAfter error", e);
        }
        TraceId.logTraceID.remove();
        LogMessageThreadLocal.logMessageThreadLocal.remove();
    }

}
