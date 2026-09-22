package com.xiongx9527.fw.plus.ai.mcp.server;

import com.xiongx9527.fw.plus.ai.mcp.spi.McpContent;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPageRequest;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpTool;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolCallRequest;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 基于 @McpTool 注解方法的 McpToolProvider 适配器
 *
 * <p>包装标注了 {@link com.xiongx9527.fw.plus.ai.mcp.annotation.McpTool} 的 Spring Bean 方法，
 * 暴露为单个工具的 {@link McpToolProvider}，并通过 {@link McpSchemaGenerator}
 * 根据方法参数自动生成 inputSchema。
 *
 * <p>方法返回值自动包装为 {@link McpToolResult}：
 * <ul>
 *   <li>返回 {@link McpToolResult}：直接返回</li>
 *   <li>返回 {@link McpContent} / {@code List<McpContent>}：包装为成功结果</li>
 *   <li>返回 String：包装为文本内容</li>
 *   <li>其他对象：toString() 后包装为文本内容</li>
 *   <li>抛异常：包装为 isError=true 的错误结果（不破坏 JSON-RPC 层）</li>
 * </ul>
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public class MethodMcpTool implements McpToolProvider {

	private static final Logger logger = LoggerFactory.getLogger(MethodMcpTool.class);

	private final Object bean;
	private final Method method;
	private final McpTool toolDefinition;

	public MethodMcpTool(Object bean, Method method, McpTool toolDefinition) {
		if (bean == null || method == null || toolDefinition == null) {
			throw new IllegalArgumentException("bean, method and toolDefinition must not be null");
		}
		this.bean = bean;
		this.method = method;
		this.toolDefinition = toolDefinition;
		// 启动期快速失败：校验方法参数类型并生成 inputSchema
		Map<String, Object> schema = McpSchemaGenerator.generateForMethod(method);
		this.toolDefinition.setInputSchema(schema);
	}

	@Override
	public List<McpTool> listTools(McpProviderContext context, McpPageRequest pageRequest) {
		return Collections.singletonList(toolDefinition);
	}

	@Override
	public McpToolResult callTool(McpProviderContext context, McpToolCallRequest request) {
		try {
			Object[] args = McpSchemaGenerator.bindArguments(method, request.getArguments());
			Object result = method.invoke(bean, args);
			return wrapResult(result);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause() != null ? e.getCause() : e;
			logger.error("Tool '{}' invocation threw", toolDefinition.name(), cause);
			return McpToolResult.error(cause.getMessage() != null ? cause.getMessage() : cause.toString());
		} catch (Exception e) {
			logger.error("Tool '{}' invocation failed", toolDefinition.name(), e);
			return McpToolResult.error(e.getMessage() != null ? e.getMessage() : e.toString());
		}
	}

	/**
	 * 将方法返回值包装为 McpToolResult
	 */
	@SuppressWarnings("unchecked")
	private McpToolResult wrapResult(Object result) {
		if (result == null) {
			return McpToolResult.success("");
		}
		if (result instanceof McpToolResult) {
			return (McpToolResult) result;
		}
		if (result instanceof McpContent) {
			List<McpContent> contents = new ArrayList<>();
			contents.add((McpContent) result);
			return McpToolResult.success(contents);
		}
		if (result instanceof List) {
			List<?> list = (List<?>) result;
			if (!list.isEmpty() && list.get(0) instanceof McpContent) {
				List<McpContent> contents = new ArrayList<>();
				for (Object item : list) {
					contents.add((McpContent) item);
				}
				return McpToolResult.success(contents);
			}
		}
		return McpToolResult.success(result.toString());
	}

	/**
	 * 获取被包装的工具定义
	 */
	public McpTool toolDefinition() {
		return toolDefinition;
	}

	/**
	 * 获取被包装的目标 Bean
	 */
	public Object bean() {
		return bean;
	}

	/**
	 * 获取被包装的方法
	 */
	public Method method() {
		return method;
	}

	/**
	 * 获取方法参数（用于测试与诊断）
	 */
	Parameter[] parameters() {
		return method.getParameters();
	}
}
