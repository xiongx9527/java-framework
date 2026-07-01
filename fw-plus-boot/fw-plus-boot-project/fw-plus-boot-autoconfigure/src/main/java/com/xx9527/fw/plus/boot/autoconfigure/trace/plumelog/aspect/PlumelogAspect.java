package com.xx9527.fw.plus.boot.autoconfigure.trace.plumelog.aspect;

import com.xx9527.fw.plus.boot.autoconfigure.trace.plumelog.PlumelogTrace;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class PlumelogAspect {

    private final PlumelogTrace plumelogTrace;

    public PlumelogAspect(PlumelogTrace plumelogTrace) {
        this.plumelogTrace = plumelogTrace;
    }

    @Around("@annotation(com.xx9527.fw.plus.boot.autoconfigure.trace.plumelog.annotation.PlumelogTrace)")
    public Object beforeExecute(ProceedingJoinPoint joinPoint) {
        return plumelogTrace.traceLog(joinPoint.getSignature().toShortString(), o -> {
            try {
                return joinPoint.proceed();
            } catch (Throwable e) {
                if (e instanceof Error) {
                    throw (Error) e;
                }
                if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;
                }
                throw new RuntimeException(e);
            }
        });
    }

}
