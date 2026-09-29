package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code langchain4j} 脚本模块的装配：只把模块注册成 magic-api 脚本模块。
 *
 * <p>模块本身不注入任何依赖——模型/记忆/工具/检索器都由脚本按参数传入（见 {@link AssistantBuilder}），
 * 因此这里只有一个无参 Bean，各插件类型可独立启停。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.service.AiServices")
@Configuration(proxyBeanMethods = false)
public class Langchain4jModuleConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Langchain4jModule langchain4jModule() {
		return new Langchain4jModule();
	}
}
