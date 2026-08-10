package com.xiongx9527.fw.plus.tinyengine.common.exception;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class ServiceException extends BizException {

	private static final long serialVersionUID = 1L;

	private final String serviceCode;

	public ServiceException(String code, String message) {
		super(message);
		this.serviceCode = code;
	}

	public String getServiceCode() {
		return serviceCode;
	}
}
