package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.github-models", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.model.githubmodels.spring.AutoConfig.class)
public class LangChain4jGitHubModelsAutoConfiguration {
}
