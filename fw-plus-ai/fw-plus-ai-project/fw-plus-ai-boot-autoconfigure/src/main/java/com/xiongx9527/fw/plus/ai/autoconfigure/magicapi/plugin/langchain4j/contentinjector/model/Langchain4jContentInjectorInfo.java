package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.contentinjector.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

/**
 * {@code langchain4j-content-injector} 资源定义：一份 {@code .ms} 资源即一个检索内容注入器。
 *
 * <p>继承 {@link PathMagicEntity}：{@code path} 为资源路径，{@code script} 为注入逻辑脚本
 * （magic-script，返回注入后的文本或 {@code ChatMessage}）；{@code params} 为控制台执行测试时
 * 注入脚本上下文的测试参数（JSON 对象字符串）。由 {@code Langchain4jContentInjectorModule}
 * 求值并暴露给 {@code DefaultRetrievalAugmentor.contentInjector(...)} 装配。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Langchain4jContentInjectorInfo extends PathMagicEntity {

	/**
	 * 展示描述。
	 */
	private String description;

	/**
	 * 测试参数（JSON 对象字符串），控制台执行测试时解析为 Map 注入脚本上下文。
	 */
	private String params;

	@Override
	public Langchain4jContentInjectorInfo simple() {
		Langchain4jContentInjectorInfo info = new Langchain4jContentInjectorInfo();
		super.simple(info);
		info.setParams(this.params);
		info.setDescription(this.description);
		return info;
	}

	@Override
	public Langchain4jContentInjectorInfo copy() {
		Langchain4jContentInjectorInfo info = new Langchain4jContentInjectorInfo();
		super.copyTo(info);
		info.setParams(this.params);
		info.setDescription(this.description);
		return info;
	}
}
