package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.trace;

import com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.PlumelogTrace;
import lombok.extern.slf4j.Slf4j;
import org.ssssssss.magicapi.core.annotation.MagicModule;

import java.util.function.Function;

@Slf4j
@MagicModule("trace")
public class TraceModule {

    /**
     * PlumelogTrace实例，用于记录和追踪日志信息
     * 该字段为final类型，确保在对象创建后不可更改
     */
    private final PlumelogTrace trace;

    /**
     * TraceModule构造函数
     * @param trace PlumelogTrace实例，用于执行具体的追踪操作
     */
    public TraceModule(PlumelogTrace trace) {
        this.trace = trace;
    }

    /**
     * 开始追踪日志记录
     * @param messageType 消息类型标识
     * @return 操作是否成功
     */
    public boolean startTraceLog(String messageType) {
        return this.trace.startTraceLog(messageType);
    }

    /**
     * 使用指定的日志器开始追踪日志记录
     * @param log SLF4J日志器实例
     * @param messageType 消息类型标识
     * @return 操作是否成功
     */
    public boolean startTraceLog(org.slf4j.Logger log,String messageType) {
        return this.trace.startTraceLog(log, messageType);
    }

    /**
     * 结束追踪日志记录
     * @param messageType 消息类型标识
     * @return 操作是否成功
     */
    public boolean endTraceLog(String messageType) {
        return this.trace.endTraceLog(messageType);
    }

    /**
     * 使用指定的日志器结束追踪日志记录
     * @param log SLF4J日志器实例
     * @param messageType 消息类型标识
     * @return 操作是否成功
     */
    public boolean endTraceLog(org.slf4j.Logger log,String messageType) {
        return this.trace.endTraceLog(log,messageType);
    }

    /**
     * 执行带追踪的日志记录操作
     * @param messageType 消息类型标识
     * @param function 要执行的函数式操作
     * @return 函数执行结果
     */
    public Object traceLog(String messageType, Function<?, ?> function) {
        return this.trace.traceLog(messageType, function);
    }

    /**
     * 使用指定的日志器执行带追踪的日志记录操作
     * @param log SLF4J日志器实例
     * @param messageType 消息类型标识
     * @param function 要执行的函数式操作
     * @return 函数执行结果
     */
    public Object traceLog(org.slf4j.Logger log,String messageType,Function<?, ?> function) {
        return this.trace.traceLog(log,messageType, function);
    }

    /**
     * 获取当前追踪ID
     * @return 追踪ID字符串
     */
    public String getTraceId() {
        return this.trace.getTraceId();
    }

    /**
     * 获取当前跨度ID
     * @return 跨度ID字符串
     */
    public String getSpanId() {
        return this.trace.getSpanId();
    }

}
