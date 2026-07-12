package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.core.rag;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Getter
@Setter
@ConfigurationProperties(prefix = RagProperties.PREFIX)
public class RagProperties {

    static final String PREFIX = "langchain4j.rag";

    @NestedConfigurationProperty
    RetrievalProperties retrieval;
}
