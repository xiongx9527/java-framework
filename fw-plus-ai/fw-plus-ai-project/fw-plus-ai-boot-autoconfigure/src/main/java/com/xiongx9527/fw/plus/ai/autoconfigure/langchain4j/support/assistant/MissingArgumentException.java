package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.assistant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * 工具调用必填参数缺失异常。
 *
 * <p>langchain4j 0.35 的 {@code DefaultToolExecutor.prepareArguments} 对缺失参数静默填
 * {@code null}（连异常都不抛），{@link GuardedToolExecutor} 因此在执行前按
 * {@code ToolSpecification.parameters().required()} 主动检测并抛出本异常，
 * 再交由 {@link ToolArgumentsErrorHandler} 决定回给 LLM 的文本
 * （默认提示向用户反问缺失参数）。语义对齐 langchain4j 1.x 的工具参数缺失场景。</p>
 */
public class MissingArgumentException extends BizException {

	private static final long serialVersionUID = 1L;

	/** 状态码 40001001（CLIENT/REQUEST/001 工具调用必填参数缺失） */
	public static final int CODE = 40_001_001;

	private final String toolName;

	private final List<String> missingParameters;

	public MissingArgumentException(String toolName, List<String> missingParameters) {
		super(CODE, String.format("工具 '%s' 缺少必填参数：%s", toolName, missingParameters));
		this.toolName = toolName;
		this.missingParameters = Collections.unmodifiableList(new ArrayList<String>(missingParameters));
	}

	/** 缺失参数对应的工具名 */
	public String getToolName() {
		return toolName;
	}

	/** 缺失的必填参数名列表 */
	public List<String> getMissingParameters() {
		return missingParameters;
	}

}
