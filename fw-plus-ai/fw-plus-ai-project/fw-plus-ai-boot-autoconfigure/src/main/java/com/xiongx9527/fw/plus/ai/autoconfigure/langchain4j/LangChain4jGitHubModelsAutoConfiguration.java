package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass(name = {"dev.langchain4j.model.github.GitHubModelsChatModel", "dev.langchain4j.model.github.GitHubModelsStreamingChatModel", "dev.langchain4j.model.github.GitHubModelsEmbeddingModel"})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.github-models", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.model.githubmodels.spring.AutoConfig.class)
public class LangChain4jGitHubModelsAutoConfiguration {
}
