package com.xiongx9527.fw.plus.ai.spring.adapter;

import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelType;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

abstract class SpringAiModel implements AiModel {

    static final int MAX_CONCURRENT_CALLS = 8;

    private static final AtomicInteger THREAD_SEQUENCE = new AtomicInteger();
    private static final ThreadPoolExecutor CALL_EXECUTOR = new ThreadPoolExecutor(0, MAX_CONCURRENT_CALLS,
            30L, TimeUnit.SECONDS, new SynchronousQueue<>(), new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable,
                            "fw-plus-spring-ai-model-call-" + THREAD_SEQUENCE.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            }, new ThreadPoolExecutor.AbortPolicy());

    static {
        CALL_EXECUTOR.allowCoreThreadTimeOut(true);
    }

    private final AiModelDefinition definition;

    SpringAiModel(AiModelDefinition definition) {
        this.definition = definition;
    }

    @Override
    public String getKey() {
        return definition.getKey();
    }

    @Override
    public String getFramework() {
        return definition.getFramework();
    }

    @Override
    public String getProvider() {
        return definition.getProvider();
    }

    @Override
    public AiModelType getType() {
        return definition.getType();
    }

    protected <T> T execute(long timeoutMillis, Callable<T> call) {
        if (timeoutMillis <= 0L) {
            try {
                return call.call();
            } catch (AiModelException exception) {
                throw safeModelException(exception);
            } catch (Exception exception) {
                throw classify(exception);
            }
        }
        Future<T> task;
        try {
            task = CALL_EXECUTOR.submit(call);
        } catch (RejectedExecutionException exception) {
            throw AiModelException.infrastructureFailure(context("concurrency limit reached"),
                    sanitizedCause(exception));
        }
        try {
            return task.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            task.cancel(true);
            throw AiModelException.infrastructureFailure(context("timed out"), exception);
        } catch (InterruptedException exception) {
            task.cancel(true);
            Thread.currentThread().interrupt();
            throw AiModelException.infrastructureFailure(context("was interrupted"), exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw classify(cause);
        }
    }

    protected AiModelException requestRejected(String reason) {
        return AiModelException.requestRejected(context(reason), null);
    }

    private AiModelException classify(Throwable failure) {
        if (failure instanceof AiModelException) {
            return safeModelException((AiModelException) failure);
        }
        if (isInfrastructureFailure(failure)) {
            if (hasCause(failure, InterruptedException.class)) {
                Thread.currentThread().interrupt();
            }
            return AiModelException.infrastructureFailure(context("infrastructure failure"), sanitizedCause(failure));
        }
        return AiModelException.requestRejected(context("request rejected"), sanitizedCause(failure));
    }

    private AiModelException safeModelException(AiModelException failure) {
        RuntimeException cause = sanitizedCause(failure);
        if (AiModelException.MODEL_INFRASTRUCTURE_FAILURE.equals(failure.getErrorCode()) || failure.isRetryable()) {
            return AiModelException.infrastructureFailure(context("infrastructure failure"), cause);
        }
        if (AiModelException.MODEL_REQUEST_REJECTED.equals(failure.getErrorCode())) {
            return AiModelException.requestRejected(context("request rejected"), cause);
        }
        return new AiModelException(failure.getErrorCode(), false, context("failed"), cause);
    }

    private RuntimeException sanitizedCause(Throwable failure) {
        return new IllegalStateException("Upstream failure type: " + failure.getClass().getName());
    }

    private boolean isInfrastructureFailure(Throwable failure) {
        return hasCause(failure, TimeoutException.class)
                || hasCause(failure, IOException.class)
                || hasCause(failure, InterruptedException.class);
    }

    private boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        Throwable current = failure;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String context(String reason) {
        return "Spring AI model " + reason + ": key=" + getKey() + ", framework=" + getFramework()
                + ", provider=" + getProvider() + ", type=" + getType();
    }
}
