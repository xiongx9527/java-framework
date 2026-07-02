package com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.aspect;

import com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.PlumelogTrace;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

import java.util.function.Function;

@Slf4j
@Aspect
public class PlumelogAspect {

    /**
     * PlumelogTrace实例，用于记录和追踪日志信息
     */
    private final PlumelogTrace plumelogTrace ;

    /**
     * 构造函数，初始化PlumelogAspect切面类
     *
     * @param plumelogTrace PlumelogTrace实例，用于日志追踪功能
     */
    public PlumelogAspect(PlumelogTrace plumelogTrace) {
        this.plumelogTrace = plumelogTrace;
    }

    /**
     * 环绕通知方法，用于处理带有PlumelogTrace注解的方法的链路追踪
     * 该方法会在被@PlumelogTrace注解标记的方法执行前后进行拦截处理
     *
     * @param joinPoint 连接点对象，包含了被拦截方法的相关信息
     * @return 被拦截方法的执行结果
     */
    @Around("@annotation(com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.annotation.PlumelogTrace))")
    public Object beforeExecute(JoinPoint joinPoint) {
        return plumelogTrace.traceLog(joinPoint.getSignature().toString(),new Function(){
            @Override
            public Object apply(Object o) {
                // 执行被拦截的目标方法，并处理可能抛出的异常
                try {
                    return ((ProceedingJoinPoint) joinPoint).proceed();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

}
