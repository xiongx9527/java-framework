package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.ssssssss.magicapi.core.exception.InvalidArgumentException;
import org.ssssssss.magicapi.core.exception.MagicLoginException;
import org.ssssssss.magicapi.core.model.JsonBean;

@RestControllerAdvice(basePackages = {
		"org.ssssssss.magicapi",
		"com.xiongx9527.fw.plus.boot.autoconfigure.magicapi"
})
public class MagicAPIWebFluxExceptionHandler {

	private static final Logger logger = LoggerFactory.getLogger(MagicAPIWebFluxExceptionHandler.class);

	@ExceptionHandler(MagicLoginException.class)
	public JsonBean<Void> invalidLogin(MagicLoginException exception) {
		return new JsonBean<>(401, exception.getMessage());
	}

	@ExceptionHandler(InvalidArgumentException.class)
	public JsonBean<Void> invalidArgument(InvalidArgumentException exception) {
		return new JsonBean<>(exception.getCode(), exception.getMessage());
	}

	@ExceptionHandler(Exception.class)
	public JsonBean<Void> exception(Exception exception) {
		logger.error("magic-api调用接口出错", exception);
		return new JsonBean<>(-1, exception.getMessage());
	}
}
