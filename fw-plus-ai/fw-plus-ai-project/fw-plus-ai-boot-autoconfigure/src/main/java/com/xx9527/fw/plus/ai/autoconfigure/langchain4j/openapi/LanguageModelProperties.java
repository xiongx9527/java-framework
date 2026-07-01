package com.xx9527.fw.plus.ai.autoconfigure.langchain4j.openapi;

import java.time.Duration;
import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
@Data
class LanguageModelProperties {
    String baseUrl;
    String apiKey;
    String organizationId;
    String modelName;
    Double temperature;
    Duration timeout;
    Integer maxRetries;
    @NestedConfigurationProperty
    ProxyProperties proxy;
    Boolean logRequests;
    Boolean logResponses;
    Map<String, String> customHeaders;
}
