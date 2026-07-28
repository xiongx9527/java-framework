//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package dev.langchain4j.openai.spring;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

class ChatModelProperties {
    String baseUrl;
    String apiKey;
    String organizationId;
    String modelName;
    Double temperature;
    Double topP;
    List<String> stop;
    Integer maxTokens;
    Integer maxCompletionTokens;
    Double presencePenalty;
    Double frequencyPenalty;
    Map<String, Integer> logitBias;
    String responseFormat;
    Boolean strictJsonSchema;
    Integer seed;
    String user;
    Boolean strictTools;
    Boolean parallelToolCalls;
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

    public Double getTopP() {
        return this.topP;
    }

    public List<String> getStop() {
        return this.stop;
    }

    public Integer getMaxTokens() {
        return this.maxTokens;
    }

    public Integer getMaxCompletionTokens() {
        return this.maxCompletionTokens;
    }

    public Double getPresencePenalty() {
        return this.presencePenalty;
    }

    public Double getFrequencyPenalty() {
        return this.frequencyPenalty;
    }

    public Map<String, Integer> getLogitBias() {
        return this.logitBias;
    }

    public String getResponseFormat() {
        return this.responseFormat;
    }

    public Boolean getStrictJsonSchema() {
        return this.strictJsonSchema;
    }

    public Integer getSeed() {
        return this.seed;
    }

    public String getUser() {
        return this.user;
    }

    public Boolean getStrictTools() {
        return this.strictTools;
    }

    public Boolean getParallelToolCalls() {
        return this.parallelToolCalls;
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

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public void setStop(List<String> stop) {
        this.stop = stop;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public void setMaxCompletionTokens(Integer maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens;
    }

    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public void setLogitBias(Map<String, Integer> logitBias) {
        this.logitBias = logitBias;
    }

    public void setResponseFormat(String responseFormat) {
        this.responseFormat = responseFormat;
    }

    public void setStrictJsonSchema(Boolean strictJsonSchema) {
        this.strictJsonSchema = strictJsonSchema;
    }

    public void setSeed(Integer seed) {
        this.seed = seed;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setStrictTools(Boolean strictTools) {
        this.strictTools = strictTools;
    }

    public void setParallelToolCalls(Boolean parallelToolCalls) {
        this.parallelToolCalls = parallelToolCalls;
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
