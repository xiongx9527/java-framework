package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web;

import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAccessDeniedException;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnauthenticatedException;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnsupportedCapabilityException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;

public final class TinyEngineWebResponses {

	private TinyEngineWebResponses() {
	}

	public static <T> ResponseEntity<Result<T>> ok(T data) {
		return ResponseEntity.ok(Result.success(data));
	}

	public static ResponseEntity<Result<Void>> ok() {
		return ResponseEntity.ok(Result.success());
	}

	public static ResponseEntity<Result<Object>> notFound(String message) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.failed("404", message));
	}

	public static ResponseEntity<Result<Object>> error(Throwable ex) {
		if (ex instanceof TinyEngineUnauthenticatedException) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Result.failed("401", ex.getMessage()));
		}
		if (ex instanceof TinyEngineAccessDeniedException) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.failed("403", ex.getMessage()));
		}
		if (ex instanceof TinyEngineUnsupportedCapabilityException) {
			return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Result.failed("501", ex.getMessage()));
		}
		if (ex instanceof TimeoutException || ex instanceof RejectedExecutionException) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body(Result.failed("503", "TinyEngine service is busy"));
		}
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Result.failed("500", ex.getMessage()));
	}
}
