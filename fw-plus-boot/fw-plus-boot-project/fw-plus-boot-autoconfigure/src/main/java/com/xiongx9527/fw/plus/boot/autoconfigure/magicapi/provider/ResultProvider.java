package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider;

import com.xiongx9527.fw.plus.common.exception.BizException;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.common.trace.AbstractTrace;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.ResponseCode;
import org.ssssssss.magicapi.core.context.RequestEntity;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.modules.db.model.Page;
import org.ssssssss.magicapi.modules.db.model.PageResult;

import java.util.List;
import java.util.Map;

public class ResultProvider implements org.ssssssss.magicapi.core.interceptor.ResultProvider {

	/**
	 * MagicAPI配置属性对象，用于获取响应码等配置信息
	 */
	private final MagicAPIProperties properties;

	
	/**
	 * 构造方法，注入MagicAPI配置属性
	 *
	 * @param properties MagicAPI配置属性对象，包含API的各种配置参数
	 */
	public ResultProvider(MagicAPIProperties properties) {
        this.properties = properties;
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
		// 创建JsonBean对象，封装状态码、消息、数据和响应时间
		JsonBean jsonBean =  new JsonBean<>(code, message, data, (int) (System.currentTimeMillis() - requestEntity.getRequestTime()));
		// 返回封装好的JsonBean对象
		jsonBean.setTraceId(AbstractTrace.get().getTraceId());
		jsonBean.setSpanId(AbstractTrace.get().getSpanId());
		ResponseCode responseCodeConfig = this.properties.getResponseCode();
		jsonBean.renameCode(responseCodeConfig.getCode());
		jsonBean.renameMessage(responseCodeConfig.getMessage());
		jsonBean.renameData(responseCodeConfig.getData());
		jsonBean.renameTraceId(responseCodeConfig.getTraceId());
		jsonBean.renameSpanId(responseCodeConfig.getSpanId());
		jsonBean.renameTimestamp(responseCodeConfig.getTimestamp());
		jsonBean.renameExecuteTime(responseCodeConfig.getExecuteTime());
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
		Object result = null;
		if (throwable instanceof BizException) {
			BizException bizException = (BizException) throwable;
			// BizException.getMessage() 已按当前请求语言（Accept-Language / ?lang=）解析文案，
			// 前端再据 code 做二次本地化兜底，实现后端文案 + 前端 code 的双保险
			result = this.buildResult(requestEntity, bizException.getCode(), bizException.getMessage(), bizException.getData());
		} else {
			result = this.buildResult(requestEntity, Constants.RESPONSE_CODE_EXCEPTION, I18nMessageResolver.resolve(50099900));
		}
		if (null !=result && result instanceof JsonBean) {
			((JsonBean<?>) result).setSuccess(false);
		}
		return result;
	}

	/**
	 * 构建分页查询结果的方法
	 *
	 * 该方法用于将分页查询的数据封装成统一的分页响应格式，包含总记录数和当前页数据列表
	 * 同时支持根据配置自定义响应字段名称
	 *
	 * @param requestEntity 请求实体，包含请求的相关信息
	 * @param page 分页对象，包含分页参数信息
	 * @param total 总记录数，表示满足查询条件的数据总量
	 * @param data 当前页的数据列表，以Map形式存储每行数据
	 * @return 返回一个PageResult对象，包含格式化后的分页数据
	 */
	@Override
	public Object buildPageResult(RequestEntity requestEntity, Page page, long total, List<Map<String, Object>> data) {
		org.ssssssss.magicapi.core.config.Page pageConfig = this.properties.getPage();
		PageResult pageResult = new PageResult(total, data);
		pageResult.renameList(pageConfig.getList());
		pageResult.renameTotal(pageConfig.getTotal());
		return pageResult;
	}


}
