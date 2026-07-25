//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.langchain4j.openai.spring;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

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

    public String getBaseUrl() {
        return this.baseUrl;
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public String getOrganizationId() {
        return this.organizationId;
    }

    public String getModelName() {
        return this.modelName;
    }

    public Double getTemperature() {
        return this.temperature;
    }

    public Duration getTimeout() {
        return this.timeout;
    }

    public Integer getMaxRetries() {
        return this.maxRetries;
    }

    public ProxyProperties getProxy() {
        return this.proxy;
    }

    public Boolean getLogRequests() {
        return this.logRequests;
    }

    public Boolean getLogResponses() {
        return this.logResponses;
    }

    public Map<String, String> getCustomHeaders() {
        return this.customHeaders;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public void setProxy(ProxyProperties proxy) {
        this.proxy = proxy;
    }

    public void setLogRequests(Boolean logRequests) {
        this.logRequests = logRequests;
    }

    public void setLogResponses(Boolean logResponses) {
        this.logResponses = logResponses;
    }

    public void setCustomHeaders(Map<String, String> customHeaders) {
        this.customHeaders = customHeaders;
    }
}
