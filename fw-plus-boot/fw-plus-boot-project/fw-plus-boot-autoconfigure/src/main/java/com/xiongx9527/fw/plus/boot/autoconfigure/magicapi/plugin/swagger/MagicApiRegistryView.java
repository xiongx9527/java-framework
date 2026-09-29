package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger;

import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;

import java.util.List;

/**
 * magic-api 接口注册表的栈无关视图。
 * <p>servlet 支线的 {@code RequestMagicDynamicRegistry} 与 webflux 支线的
 * {@code WebFluxRequestMagicDynamicRegistry} 均继承 {@code AbstractMagicDynamicRegistry<ApiInfo>}，
 * 经本视图收敛后，swagger 文档生成核心不再依赖 servlet 专属类型。
 */
public interface MagicApiRegistryView {

	/**
	 * 当前已注册的 magic-api 接口列表。
	 *
	 * @return 接口信息列表
	 */
	List<ApiInfo> mappings();

	/**
	 * 将抽象注册表适配为本视图。
	 *
	 * @param registry 抽象注册表实例
	 * @return 栈无关视图
	 */
	static MagicApiRegistryView of(AbstractMagicDynamicRegistry<ApiInfo> registry) {
		return registry::mappings;
	}
}
