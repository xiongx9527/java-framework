package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import dev.langchain4j.model.github.GitHubModelsChatModel;
import dev.langchain4j.model.github.GitHubModelsEmbeddingModel;
import dev.langchain4j.model.github.GitHubModelsStreamingChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

@ConditionalOnClass({GitHubModelsChatModel.class, GitHubModelsStreamingChatModel.class, GitHubModelsEmbeddingModel.class})
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j.github-models", name = "enabled", havingValue = "true", matchIfMissing = true)
@Import(dev.langchain4j.model.githubmodels.spring.AutoConfig.class)
public class LangChain4jGitHubModelsAutoConfiguration {
}
