package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.systemmessage.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

/**
 * {@code langchain4j-system-message} 资源定义：一份 {@code .ms} 资源即一条系统提示词。
 *
 * <p>继承 {@link PathMagicEntity}：{@code path} 为资源路径，{@code script} 为系统提示词脚本
 * （magic-script，返回提示词文本）；{@code params} 为控制台执行测试时注入脚本上下文的测试参数
 * （JSON 对象字符串）。由 {@code Langchain4jSystemMessageProviderModule} 求值并暴露给
 * {@code AiServices.systemMessageProvider(...)} 装配。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jSystemMessageInfo extends PathMagicEntity {

	/**
	 * 展示描述。
	 */
	private String description;

	/**
	 * 测试参数（JSON 对象字符串），控制台执行测试时解析为 Map 注入脚本上下文。
	 */
	private String params;

	@Override
	public Langchain4jSystemMessageInfo simple() {
		Langchain4jSystemMessageInfo info = new Langchain4jSystemMessageInfo();
		super.simple(info);
		info.setParams(this.params);
		info.setDescription(this.description);
		return info;
	}

	@Override
	public Langchain4jSystemMessageInfo copy() {
		Langchain4jSystemMessageInfo info = new Langchain4jSystemMessageInfo();
		super.copyTo(info);
		info.setParams(this.params);
		info.setDescription(this.description);
		return info;
	}
}
