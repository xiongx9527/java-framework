package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelType;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LangChain4jAiModel implements AiModel {

    private final AiModelDefinition definition;
    private final LangChain4jModel nativeModel;

    public LangChain4jAiModel(AiModelDefinition definition, LangChain4jModel nativeModel) {
        this.definition = definition;
        this.nativeModel = nativeModel;
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

    public LangChain4jModel getNativeModel() {
        return nativeModel;
    }

	protected <T> T execute(long timeoutMillis, Callable<T> call) {
		if (timeoutMillis <= 0L) {
			try {
				return call.call();
			} catch (AiModelException e) {
				throw e;
			} catch (Exception e) {
				throw classify(e);
			}
		}

		FutureTask<T> task = new FutureTask<T>(call);
		Thread thread = new Thread(task, "fw-plus-langchain4j-model-call");
		thread.setDaemon(true);
		thread.start();
		try {
			return task.get(timeoutMillis, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			task.cancel(true);
			throw AiModelException.infrastructureFailure(context("timed out"), e);
		} catch (InterruptedException e) {
			task.cancel(true);
			Thread.currentThread().interrupt();
			throw AiModelException.infrastructureFailure(context("was interrupted"), e);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
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
			return (AiModelException) failure;
		}
		if (isInfrastructureFailure(failure)) {
			return AiModelException.infrastructureFailure(context("infrastructure failure"), failure);
		}
		return AiModelException.requestRejected(context("request rejected"), failure);
	}

	private boolean isInfrastructureFailure(Throwable failure) {
		Throwable current = failure;
		while (current != null) {
			if (current instanceof TimeoutException || current instanceof IOException
					|| current instanceof InterruptedException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}

	private String context(String reason) {
		return "LangChain4j model " + reason + ": key=" + getKey() + ", type=" + getType();
	}
}
