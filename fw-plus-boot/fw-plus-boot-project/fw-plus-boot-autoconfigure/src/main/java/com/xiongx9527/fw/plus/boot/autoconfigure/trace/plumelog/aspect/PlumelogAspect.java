package com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.aspect;

import com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.PlumelogTrace;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class PlumelogAspect {

    private final PlumelogTrace plumelogTrace;

    public PlumelogAspect(PlumelogTrace plumelogTrace) {
        this.plumelogTrace = plumelogTrace;
    }

    @Around("@annotation(com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.annotation.PlumelogTrace)")
    public Object beforeExecute(ProceedingJoinPoint joinPoint) throws Throwable {
        String messageType = joinPoint.getSignature().toShortString();
        boolean start = plumelogTrace.startTraceLog(messageType);
        try {
            return joinPoint.proceed();
        } finally {
            if (start) {
                plumelogTrace.endTraceLog(messageType);
            }
        }
    }

}
