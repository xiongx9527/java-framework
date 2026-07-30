package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine;

import com.xiongx9527.fw.plus.tinyengine.support.context.SystemTinyEngineUserContext;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import com.xiongx9527.fw.plus.tinyengine.support.hook.NoopTinyEngineSaveHook;
import com.xiongx9527.fw.plus.tinyengine.support.hook.TinyEngineSaveHook;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(TinyEngineUserContext.class)
public class TinyEngineCoreConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public TinyEngineUserContext tinyEngineUserContext() {
		return new SystemTinyEngineUserContext();
	}

	@Bean
	@ConditionalOnMissingBean
	public TinyEngineSaveHook tinyEngineSaveHook() {
		return new NoopTinyEngineSaveHook();
	}
}
