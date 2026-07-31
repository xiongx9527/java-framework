package com.xiongx9527.fw.plus.ai.askdata.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.AskDataAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.LangChain4jModelAskDataAutoConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import org.junit.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.util.ClassUtils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class AskDataStarterContextTest {
	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(LangChain4jModelAskDataAutoConfiguration.class,
					AskDataAutoConfiguration.class));

	@Test
	public void shouldStartMinimalContextWithAskDataStarterOnlyClasspath() {
		assertFalse(ClassUtils.isPresent("dev.langchain4j.model.chat.ChatLanguageModel",
				getClass().getClassLoader()));
		contextRunner.run(context -> {
			assertTrue(context.isActive());
			assertNotNull(context.getBean(AskDataModelRegistry.class));
			assertNotNull(context.getBean(com.xiongx9527.fw.plus.ai.askdata.api.AskDataService.class));
		});
	}
}
