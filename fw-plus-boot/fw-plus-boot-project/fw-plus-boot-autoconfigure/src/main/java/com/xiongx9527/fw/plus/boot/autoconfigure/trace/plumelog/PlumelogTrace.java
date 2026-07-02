package com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog;

import com.xiongx9527.fw.plus.boot.autoconfigure.trace.opentelemetry.OpentelemetryTrace;
import com.xiongx9527.fw.plus.common.trace.AbstractTrace;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.plumelog.core.LogMessageThreadLocal;
import com.plumelog.core.TraceId;
import com.plumelog.core.TraceMessage;
import com.plumelog.core.constant.LogMessageConstant;
import com.plumelog.core.util.GfJsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;


@Slf4j
public class PlumelogTrace extends AbstractTrace {

    /**
     * 开始追踪日志记录 - 使用默认logger
     * @param messageType 消息类型
     * @return 是否成功开始追踪日志
     */
    public boolean startTraceLog(String messageType) {
        return startTraceLog(log, messageType);
    }

    /**
     * 开始追踪日志记录 - 使用指定logger
     * @param log 日志记录器
     * @param messageType 消息类型
     * @return 是否成功开始追踪日志
     */
    public boolean startTraceLog(org.slf4j.Logger log,String messageType) {
        try {
            String traceId = TraceId.logTraceID.get();
            if (StringUtils.isNotBlank(traceId)) {
                TraceMessage traceMessage = LogMessageThreadLocal.logMessageThreadLocal.get();
                if (traceMessage == null) {
                    traceMessage = new TraceMessage();
                    traceMessage.getPositionNum().set(0);
                }
                traceMessage.setTraceId(traceId);
                traceMessage.setMessageType(messageType);
                traceMessage.setPosition(LogMessageConstant.TRACE_START);
                traceMessage.getPositionNum().incrementAndGet();
                LogMessageThreadLocal.logMessageThreadLocal.set(traceMessage);
                log.info(LogMessageConstant.TRACE_PRE + GfJsonUtil.toJSONString(traceMessage));
                return true;
            }
        } catch (Exception e) {
            log.error("startTraceLog error", e);
        }
        return false;
    }

    /**
     * 结束追踪日志记录 - 使用默认logger
     * @param messageType 消息类型
     * @return 是否成功结束追踪日志
     */
    public boolean endTraceLog(String messageType) {
        return endTraceLog(log, messageType);
    }

    /**
     * 结束追踪日志记录 - 使用指定logger
     * @param log 日志记录器
     * @param messageType 消息类型
     * @return 是否成功结束追踪日志
     */
    public boolean endTraceLog(org.slf4j.Logger log,String messageType) {
        try {
            String traceId = TraceId.logTraceID.get();
            if (StringUtils.isNotBlank(traceId)) {
                TraceMessage traceMessage = LogMessageThreadLocal.logMessageThreadLocal.get();
                if (traceMessage != null) {
                    traceMessage.setMessageType(messageType);
                    traceMessage.setPosition(LogMessageConstant.TRACE_END);
                    traceMessage.getPositionNum().incrementAndGet();
                    log.info(LogMessageConstant.TRACE_PRE + GfJsonUtil.toJSONString(traceMessage));
                    return true;
                }
            }
        } catch (Exception e) {
            log.error("endTraceLog error", e);
        }
        return false;
    }

    /**
     * 执行带追踪日志的函数 - 使用默认logger
     * @param messageType 消息类型
     * @param function 要执行的函数
     * @return 函数执行结果
     */
    public Object traceLog(String messageType, Function<?, ?> function) {
        return traceLog(log, messageType, function);
    }

    /**
     * 执行带追踪日志的函数 - 使用指定logger
     * @param log 日志记录器
     * @param messageType 消息类型
     * @param function 要执行的函数
     * @return 函数执行结果
     */
    public Object traceLog(org.slf4j.Logger log,String messageType,Function<?, ?> function) {
        boolean start = startTraceLog(log, messageType);
        try {
            return function.apply(null);
        } catch (Throwable throwable) {
            throw throwable;
        } finally {
            if (start) {
                endTraceLog(log, messageType);
            }
        }
    }

    @Override
    public String getTraceId() {
        String traceId = null;
        try {
            traceId = SpringUtils.getBean(OpentelemetryTrace.class).getTraceId();
        } catch (Exception e) {
        }
        if (StringUtils.isBlank(traceId)) {
            traceId = TraceId.logTraceID.get();
        }
        return traceId;
    }

    @Override
    public String getSpanId() {
        try {
            return SpringUtils.getBean(OpentelemetryTrace.class).getSpanId();
        } catch (Exception e) {
            return "";
        }
    }

}
