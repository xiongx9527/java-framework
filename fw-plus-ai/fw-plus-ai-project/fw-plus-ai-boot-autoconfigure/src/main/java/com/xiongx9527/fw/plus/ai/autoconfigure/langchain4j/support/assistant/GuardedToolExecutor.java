package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import dev.langchain4j.service.tool.ToolExecutor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 带参数错误拦截的工具执行器：执行前按 spec 的 required 列表检测缺失参数，
 * 执行中捕获参数绑定失败，统一交给 {@link ToolArgumentsErrorHandler} 决定回给 LLM 的文本。
 *
 * <p>存在原因：langchain4j 0.35 的 {@code DefaultToolExecutor.prepareArguments}
 * 对缺失参数静默填 {@code null} 继续执行，对类型不匹配抛 {@link IllegalArgumentException}
 * 且位于 try 块之外（直接炸给业务调用方）。包装层在 {@code AiServices.tools(...)} 与
 * {@code AiServices.toolProvider(...)} 两条注册路径上补齐拦截（1.x 原生
 * {@code toolArgumentsErrorHandler} 的等价物）。</p>
 *
 * <p>缺参检测只针对 spec 的 required 参数（0.35 中参数无 {@code @P} 注解或
 * {@code @P.required=true} 即必填），optional 参数缺失不拦截；
 * 工具方法执行期的业务异常仍由 {@code DefaultToolExecutor} 捕获并把 message 回给 LLM（原生行为不变）；
 * 无消息异常导致的 {@code null} 结果在本出口兜底为占位文本；
 * {@link LlmVisibleToolExecutionException} 的消息不经 handler 直接回传给 LLM。</p>
 */
public class GuardedToolExecutor implements ToolExecutor {

	private final ToolSpecification specification;

	/** toolProvider 路径：委托的原执行器（与 toolObject 二选一） */
	private final ToolExecutor delegate;

	/** 对象路径：{@code @Tool} 宿主对象，执行时按请求懒构造 {@link DefaultToolExecutor} */
	private final Object toolObject;

	private final ToolArgumentsErrorHandler errorHandler;

	public GuardedToolExecutor(ToolSpecification specification, ToolExecutor delegate,
			ToolArgumentsErrorHandler errorHandler) {
		this(specification, delegate, null, errorHandler);
	}

	public GuardedToolExecutor(ToolSpecification specification, Object toolObject,
			ToolArgumentsErrorHandler errorHandler) {
		this(specification, null, toolObject, errorHandler);
	}

	private GuardedToolExecutor(ToolSpecification specification, ToolExecutor delegate, Object toolObject,
			ToolArgumentsErrorHandler errorHandler) {
		this.specification = specification;
		this.delegate = delegate;
		this.toolObject = toolObject;
		this.errorHandler = errorHandler != null ? errorHandler : ToolArgumentsErrorHandler.DEFAULT;
	}

	@Override
	public String execute(ToolExecutionRequest request, Object memoryId) {
		try {
			checkRequiredArguments(specification, request);
		} catch (MissingArgumentException e) {
			// 缺参：执行前拦截，handler 返回文本回给 LLM（默认提示反问用户）；handler 也可选择抛出
			return errorHandler.handle(e, request);
		}
		try {
			ToolExecutor target = delegate != null ? delegate : new DefaultToolExecutor(toolObject, request);
			String result = target.execute(request, memoryId);
			// 0.35 下工具方法抛无消息异常时 DefaultToolExecutor 返回 null（ToolExecution.result(null) 无校验），
			// 此处兜底为非空占位文本；springboot3 合并后该兜底由 1.20.2 原生 errorMessage 类名兜底覆盖，不带过去
			return result != null ? result : nullResultPlaceholder(request);
		} catch (LlmVisibleToolExecutionException e) {
			// 「抛出消息给 LLM」语义：消息强制非空，直接回传（不进 handler，属于业务异常而非参数错误）
			return e.getMessage();
		} catch (IllegalArgumentException e) {
			// 参数绑定/工具方法解析失败：回给 LLM 修正重试，而不是炸给业务调用方
			return errorHandler.handle(e, request);
		}
	}

	/** 工具执行返回 null 时回给 LLM 的占位文本（保证 ToolExecution.result 非空） */
	private static String nullResultPlaceholder(ToolExecutionRequest request) {
		return String.format("工具 '%s' 执行返回空结果，请向用户说明未获得有效数据", request.name());
	}

	/**
	 * 必填参数检测：spec 的 required 列表与请求 arguments 的键集合比对，缺任何一项即抛出。
	 * arguments 为 null/空时按「未提供任何参数」处理；非法 JSON 交由后续绑定阶段兜底。
	 */
	private static void checkRequiredArguments(ToolSpecification specification, ToolExecutionRequest request) {
		if (specification == null || specification.parameters() == null) {
			return;
		}
		List<String> required = specification.parameters().required();
		if (required == null || required.isEmpty()) {
			return;
		}
		Set<String> present = argumentKeys(request.arguments());
		List<String> missing = new ArrayList<String>();
		for (String name : required) {
			if (!present.contains(name)) {
				missing.add(name);
			}
		}
		if (!missing.isEmpty()) {
			throw new MissingArgumentException(request.name(), missing);
		}
	}

	/** 解析 arguments JSON 的顶层键集合；null/空返回空集，非法 JSON 返回空集（交给绑定阶段报错） */
	private static Set<String> argumentKeys(String arguments) {
		Set<String> keys = new HashSet<String>();
		if (arguments == null || arguments.trim().isEmpty()) {
			return keys;
		}
		try {
			JSONObject json = JSON.parseObject(arguments);
			if (json != null) {
				keys.addAll(json.keySet());
			}
		} catch (RuntimeException ignored) {
			// 非法 JSON：交由 DefaultToolExecutor 的参数绑定阶段处理（同样会进 handler）
		}
		return keys;
	}

}
