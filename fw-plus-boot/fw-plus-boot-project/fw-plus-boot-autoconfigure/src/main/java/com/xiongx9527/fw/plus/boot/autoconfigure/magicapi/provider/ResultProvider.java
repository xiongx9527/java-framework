package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider;

import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.model.JsonBean;

public class ResultProvider implements org.ssssssss.magicapi.core.interceptor.ResultProvider {

	/**
	 * 异常结果提供者的实例，用于处理异常情况下的结果返回
	 */
	private final ExceptionResultProvider exceptionResultProvider;

	/**
	 * 构造函数，初始化ResultProvider实例
	 *
	 * @param exceptionResultProvider 异常结果提供者，用于处理异常情况下的结果构建
	 */
	public ResultProvider(ExceptionResultProvider exceptionResultProvider) {
		this.exceptionResultProvider = exceptionResultProvider;
	}


    /**
	 * 构建返回结果的方法
	 *
	 * 该方法用于根据请求实体、状态码、消息内容和数据对象构建一个统一的响应结果
	 * 它封装了响应的所有必要信息，包括时间戳和追踪ID，以方便调用者进行统一的响应处理
	 *
	 * @param requestEntity 请求实体，包含请求的相关信息
	 * @param code 响应状态码，表示响应的状态
	 * @param message 响应消息，提供更详细的响应信息
	 * @param data 响应数据对象，包含响应的具体数据
	 * @return 返回一个Object类型的响应结果，实际上是一个JsonBean对象
	 */
	@Override
	public Object buildResult(RequestEntity requestEntity, int code, String message, Object data) {
	    // 获取当前时间戳，用于计算响应时间
	    long timestamp = System.currentTimeMillis();

	    // 创建JsonBean对象，封装状态码、消息、数据和响应时间
	    JsonBean jsonBean =  new JsonBean<>(code, message, data, (int) (timestamp - requestEntity.getRequestTime()));

	    // 返回封装好的JsonBean对象
	    return jsonBean;
	}


	/**
	 * 重写buildException方法以处理请求处理过程中的异常
	 *
	 * @param requestEntity 请求实体，包含请求的相关信息
	 * @param throwable 发生的异常或错误
	 * @return 返回一个构建好的结果对象，包含错误信息
	 */
	@Override
	public Object buildException(RequestEntity requestEntity, Throwable throwable) {
		// 默认状态码为500，表示内部服务器错误
		int code = 500;
		// 默认错误消息为异常的消息
		String message = throwable.getMessage();
		// 如果异常结果提供者不为空，则尝试使用它来构建异常结果
		if (null != exceptionResultProvider) {
			ExceptionResultProvider.ExceptionResult exceptionResult = exceptionResultProvider.buildException(requestEntity, throwable);
			// 如果构建的异常结果不为空，则使用该结果的状态码和消息
			if (null != exceptionResult) {
				code = exceptionResult.getCode();
				message = exceptionResult.getMessage();
			}
		}

		// 如果消息为空，则设置为默认的错误消息
		if (StringUtils.isEmpty(message)) {
			message = "系统内部出现错误";
		}

		// 调用buildResult方法构建结果，使用当前状态码和消息
		return buildResult(requestEntity, code, message);
	}


}
