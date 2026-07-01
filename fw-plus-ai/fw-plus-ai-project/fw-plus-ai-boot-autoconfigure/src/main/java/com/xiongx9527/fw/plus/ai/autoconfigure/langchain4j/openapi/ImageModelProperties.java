package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.openapi;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;

import lombok.Data;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Data
class ImageModelProperties {
    String baseUrl;
    String apiKey;
    String organizationId;
    String modelName;
    String size;
    String quality;
    String style;
    String user;
    String responseFormat;
    Duration timeout;
    Integer maxRetries;
    @NestedConfigurationProperty
    ProxyProperties proxy;
    Boolean logRequests;
    Boolean logResponses;
    Boolean withPersisting;
    Path persistTo;
    Map<String, String> customHeaders;
}
