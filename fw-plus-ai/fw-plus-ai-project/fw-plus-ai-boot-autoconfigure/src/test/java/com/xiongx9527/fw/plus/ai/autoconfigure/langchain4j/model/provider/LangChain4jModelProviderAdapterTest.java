package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelException;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class LangChain4jModelProviderAdapterTest {

    @Test
    public void shouldNotSupportProviderWhenRequiredClassIsMissing() {
        ReflectiveLangChain4jModelProviderAdapter adapter = new ReflectiveLangChain4jModelProviderAdapter(
                "missing", LangChain4jModelType.CHAT, "com.example.DoesNotExist", "builder");

        assertFalse(adapter.supports(definition("missing", LangChain4jModelType.CHAT)));
    }

    @Test
    public void shouldIncludeProviderAndTypeWhenCreateFails() {
        ReflectiveLangChain4jModelProviderAdapter adapter = new ReflectiveLangChain4jModelProviderAdapter(
                "openai", LangChain4jModelType.CHAT, "java.lang.String", "builder");

        try {
            adapter.create(definition("openai", LangChain4jModelType.CHAT));
            fail("Expected LangChain4jModelException");
        } catch (LangChain4jModelException e) {
            assertTrue(e.getMessage().contains("openai"));
            assertTrue(e.getMessage().contains("CHAT"));
            assertTrue(e.getMessage().contains("chat"));
        }
    }

    @Test
    public void shouldUseDirectFieldsWhenBuildingModel() {
        ReflectiveLangChain4jModelProviderAdapter adapter = new ReflectiveLangChain4jModelProviderAdapter(
                "test", LangChain4jModelType.CHAT, TestModel.class.getName(), "builder");

        LangChain4jModel model = adapter.create(definition("test", LangChain4jModelType.CHAT));

        TestModel nativeModel = (TestModel) model.getModel();
        assertEquals("http://localhost:11434/v1", nativeModel.getBaseUrl());
        assertEquals("demo", nativeModel.getApiKey());
        assertEquals("gpt-4o-mini", nativeModel.getModelName());
    }

    @Test
    public void shouldNotUseBaseUrlAsEndpointWhenEndpointIsNotConfigured() {
        ReflectiveLangChain4jModelProviderAdapter adapter = new ReflectiveLangChain4jModelProviderAdapter(
                "qianfan", LangChain4jModelType.CHAT, EndpointModel.class.getName(), "builder");

        EndpointModel nativeModel = (EndpointModel) adapter.create(definition("qianfan", LangChain4jModelType.CHAT)).getModel();

        assertEquals("http://localhost:11434/v1", nativeModel.getBaseUrl());
        assertNull(nativeModel.getEndpoint());
    }

    @Test
    public void shouldUseExplicitEndpointWhenEndpointIsConfigured() {
        ReflectiveLangChain4jModelProviderAdapter adapter = new ReflectiveLangChain4jModelProviderAdapter(
                "qianfan", LangChain4jModelType.CHAT, EndpointModel.class.getName(), "builder");
        LangChain4jModelDefinition definition = definition("qianfan", LangChain4jModelType.CHAT);
        definition.getProperties().put("endpoint", "https://qianfan.example.com");

        EndpointModel nativeModel = (EndpointModel) adapter.create(definition).getModel();

        assertEquals("https://qianfan.example.com", nativeModel.getEndpoint());
    }

    @Test
    public void shouldApplyExtensionPropertiesWithCommonTypeConversions() {
        ReflectiveLangChain4jModelProviderAdapter adapter = new ReflectiveLangChain4jModelProviderAdapter(
                "test", LangChain4jModelType.CHAT, PropertyModel.class.getName(), "builder");
        LangChain4jModelDefinition definition = definition("test", LangChain4jModelType.CHAT);
        definition.getProperties().put("temperature", "0.35");
        definition.getProperties().put("maxTokens", Long.valueOf(2048L));
        definition.getProperties().put("logRequests", "true");
        definition.getProperties().put("timeout", "PT5S");

        PropertyModel nativeModel = (PropertyModel) adapter.create(definition).getModel();

        assertEquals(0.35D, nativeModel.getTemperature(), 0.0001D);
        assertEquals(Integer.valueOf(2048), nativeModel.getMaxTokens());
        assertTrue(nativeModel.isLogRequests());
        assertEquals(Duration.ofSeconds(5L), nativeModel.getTimeout());
    }

    @Test
    public void shouldUseConfiguredBaseUrlAliasForAzureEndpoint() {
        ReflectiveLangChain4jModelProviderAdapter adapter = new ReflectiveLangChain4jModelProviderAdapter(
                "azure-openai", LangChain4jModelType.CHAT, EndpointModel.class.getName(), "builder",
                Collections.singletonMap("endpoint", "baseUrl"));

        EndpointModel nativeModel = (EndpointModel) adapter.create(
                definition("azure-openai", LangChain4jModelType.CHAT)).getModel();

        assertEquals("http://localhost:11434/v1", nativeModel.getEndpoint());
    }

    private LangChain4jModelDefinition definition(String provider, LangChain4jModelType type) {
        LangChain4jModelDefinition definition = new LangChain4jModelDefinition();
        definition.setKey("chat");
        definition.setProvider(provider);
        definition.setModelType(type);
        definition.setEnabled(true);
        definition.setBaseUrl("http://localhost:11434/v1");
        definition.setApiKey("demo");
        definition.setModelName("gpt-4o-mini");
        return definition;
    }

    public static class TestModel {

        private final String baseUrl;
        private final String apiKey;
        private final String modelName;

        private TestModel(String baseUrl, String apiKey, String modelName) {
            this.baseUrl = baseUrl;
            this.apiKey = apiKey;
            this.modelName = modelName;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public String getModelName() {
            return modelName;
        }

        public static class Builder {

            private String baseUrl;
            private String apiKey;
            private String modelName;

            public Builder baseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
                return this;
            }

            public Builder apiKey(String apiKey) {
                this.apiKey = apiKey;
                return this;
            }

            public Builder modelName(String modelName) {
                this.modelName = modelName;
                return this;
            }

            public TestModel build() {
                return new TestModel(baseUrl, apiKey, modelName);
            }
        }
    }

    public static class EndpointModel {

        private final String baseUrl;
        private final String endpoint;

        private EndpointModel(String baseUrl, String endpoint) {
            this.baseUrl = baseUrl;
            this.endpoint = endpoint;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public static class Builder {

            private String baseUrl;
            private String endpoint;

            public Builder baseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
                return this;
            }

            public Builder endpoint(String endpoint) {
                this.endpoint = endpoint;
                return this;
            }

            public EndpointModel build() {
                return new EndpointModel(baseUrl, endpoint);
            }
        }
    }

    public static class PropertyModel {

        private final double temperature;
        private final Integer maxTokens;
        private final boolean logRequests;
        private final Duration timeout;

        private PropertyModel(double temperature, Integer maxTokens, boolean logRequests, Duration timeout) {
            this.temperature = temperature;
            this.maxTokens = maxTokens;
            this.logRequests = logRequests;
            this.timeout = timeout;
        }

        public static Builder builder() {
            return new Builder();
        }

        public double getTemperature() {
            return temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public boolean isLogRequests() {
            return logRequests;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public static class Builder {

            private double temperature;
            private Integer maxTokens;
            private boolean logRequests;
            private Duration timeout;

            public Builder temperature(double temperature) {
                this.temperature = temperature;
                return this;
            }

            public Builder maxTokens(Integer maxTokens) {
                this.maxTokens = maxTokens;
                return this;
            }

            public Builder logRequests(boolean logRequests) {
                this.logRequests = logRequests;
                return this;
            }

            public Builder timeout(Duration timeout) {
                this.timeout = timeout;
                return this;
            }

            public PropertyModel build() {
                return new PropertyModel(temperature, maxTokens, logRequests, timeout);
            }
        }
    }
}
