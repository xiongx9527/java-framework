package com.xiongx9527.fw.plus.tinyengine.common.model;

import com.xiongx9527.fw.plus.tinyengine.common.exception.ExceptionEnum;
import com.xiongx9527.fw.plus.tinyengine.common.exception.IBaseError;

import java.util.LinkedHashMap;
import java.util.Map;

public class Result<T> {

	private T data;
	private String code;
	private String message;
	private boolean success;
	private Map<String, Object> error;
	private String errMsg;

	public Result() {
	}

	private Result(String code, String message, T data, boolean success) {
		this.code = code;
		this.message = message;
		this.data = data;
		this.success = success;
		if (!success) {
			this.errMsg = message;
			this.error = new LinkedHashMap<String, Object>();
			this.error.put("code", code);
			this.error.put("message", message);
		}
	}

	public static <T> Result<T> success() {
		return success(null);
	}

	public static <T> Result<T> success(T data) {
		return success(data, ExceptionEnum.SUCCESS.getResultMsg());
	}

	public static <T> Result<T> success(T data, String message) {
		return new Result<T>(ExceptionEnum.SUCCESS.getResultCode(), message, data, true);
	}

	public static <T> Result<T> failed(IBaseError error) {
		return failed(error.getResultCode(), error.getResultMsg());
	}

	public static <T> Result<T> failed(IBaseError error, String message) {
		return failed(error.getResultCode(), message);
	}

	public static <T> Result<T> failed(String errorCode, String message) {
		return new Result<T>(errorCode, message, null, false);
	}

	public static <T> Result<T> failed(String message) {
		return failed(ExceptionEnum.CM001.getResultCode(), message);
	}

	public static <T> Result<T> validateFailed(String message) {
		return failed(ExceptionEnum.CM002.getResultCode(), message);
	}

	public T getData() { return data; }
	public void setData(T data) { this.data = data; }
	public String getCode() { return code; }
	public void setCode(String code) { this.code = code; }
	public String getMessage() { return message; }
	public void setMessage(String message) { this.message = message; }
	public boolean isSuccess() { return success; }
	public void setSuccess(boolean success) { this.success = success; }
	public Map<String, Object> getError() { return error; }
	public void setError(Map<String, Object> error) { this.error = error; }
	public String getErrMsg() { return errMsg; }
	public void setErrMsg(String errMsg) { this.errMsg = errMsg; }
}
