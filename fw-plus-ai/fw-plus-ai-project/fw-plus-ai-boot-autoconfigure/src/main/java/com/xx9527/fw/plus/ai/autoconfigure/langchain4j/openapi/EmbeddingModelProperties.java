package com.xx9527.fw.plus.ai.autoconfigure.langchain4j.openapi;

import java.time.Duration;
import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
class EmbeddingModelProperties {
    String baseUrl;
    String apiKey;
    String organizationId;
    String modelName;
    Integer dimensions;
    String user;
    Duration timeout;
    Integer maxRetries;
    @NestedConfigurationProperty
    ProxyProperties proxy;
    Boolean logRequests;
    Boolean logResponses;
    Map<String, String> customHeaders;
}
