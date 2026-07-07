package com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.aspect;

import com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.PlumelogTrace;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlumelogAspectTest {

    @Test
    void beforeExecuteReturnsOriginalResult() throws Throwable {
        ProceedingJoinPoint joinPoint = mockJoinPoint("TraceTarget.success()", "ok");

        Object result = new PlumelogAspect(new PlumelogTrace()).beforeExecute(joinPoint);

        assertThat(result).isEqualTo("ok");
    }

    @Test
    void beforeExecutePropagatesOriginalCheckedException() throws Throwable {
        IOException exception = new IOException("io failed");
        ProceedingJoinPoint joinPoint = mockJoinPoint("TraceTarget.fail()", exception);

        assertThatThrownBy(() -> new PlumelogAspect(new PlumelogTrace()).beforeExecute(joinPoint))
                .isSameAs(exception);
    }

    private ProceedingJoinPoint mockJoinPoint(String signatureText, Object resultOrException) throws Throwable {
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        Signature signature = mock(Signature.class);
        when(signature.toShortString()).thenReturn(signatureText);
        when(joinPoint.getSignature()).thenReturn(signature);
        if (resultOrException instanceof Throwable) {
            when(joinPoint.proceed()).thenThrow((Throwable) resultOrException);
        } else {
            when(joinPoint.proceed()).thenReturn(resultOrException);
        }
        return joinPoint;
    }
}
