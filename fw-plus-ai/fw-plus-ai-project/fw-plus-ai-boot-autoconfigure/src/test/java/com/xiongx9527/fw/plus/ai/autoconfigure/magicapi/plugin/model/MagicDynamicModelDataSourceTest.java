package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model;

import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.ModelDataSourceInfo;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelFactory;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Collections;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicDynamicModelDataSourceTest {

    private GenericApplicationContext applicationContext;

    @AfterEach
    void tearDown() {
        if (applicationContext != null) {
            applicationContext.close();
            new SpringUtils().setApplicationContext(null);
        }
    }

    @Test
    void runtimeModelOverridesSpringBootModelAndFallsBackAfterDestroy() {
        AiModel bootModel = model("chat");
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("chat", bootModel, "chat"), factory, Collections.<AiModelAdapter>emptyList());

        dataSource.createClient(info("chat"));

        assertModel(dataSource.getModel("chat"), "chat", "langchain4j", "fake", AiModelType.CHAT);
        dataSource.destroyClientByKey("chat");
        assertModel(dataSource.getModel("chat"), "chat", "boot-framework", "boot-provider", AiModelType.CHAT);
    }

    @Test
    void runtimeModelOverridesSpringBootDefaultModel() {
        AiModel bootModel = model("chat");
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("chat", bootModel, "chat"), factory, Collections.<AiModelAdapter>emptyList());

        dataSource.createClient(info("chat"));

        assertModel(dataSource.getDefault(), "chat", "langchain4j", "fake", AiModelType.CHAT);
    }

    @Test
    void runtimeModelOverridesSpringBootDefaultGroupAlias() {
        AiModel bootModel = model("chat-default");
        Map<String, AiModel> models = new LinkedHashMap<String, AiModel>();
        models.put("chat-default", bootModel);
        models.put("chat-default.openai", bootModel);
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                new TestRegistry(models, "chat-default"), factory, Collections.<AiModelAdapter>emptyList());

        dataSource.createClient(info("chat-default"));

        assertModel(dataSource.getDefault(), "chat-default", "langchain4j", "fake", AiModelType.CHAT);
    }

    @Test
    void runtimeGroupAliasDoesNotOverrideExplicitDefaultModelNameKey() {
        AiModel bootDefaultModel = model("chat-default.qwen-plus");
        AiModel bootAliasModel = model("chat-default");
        Map<String, AiModel> models = new LinkedHashMap<String, AiModel>();
        models.put("chat-default", bootAliasModel);
        models.put("chat-default.qwen-plus", bootDefaultModel);
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                new TestRegistry(models, "chat-default.qwen-plus"), factory, Collections.<AiModelAdapter>emptyList());

        dataSource.createClient(info("chat-default"));

        assertModel(dataSource.getDefault(), "chat-default.qwen-plus", "boot-framework", "boot-provider", AiModelType.CHAT);
    }

    @Test
    void createClientRegistersMultipleRuntimeModelsWithDefaultAlias() {
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("boot", model("boot"), "boot"), factory, Collections.<AiModelAdapter>emptyList());
        ModelDataSourceInfo info = info("chat-default");
        info.setModelName(Arrays.asList("gpt-4o-mini", "qwen-plus"));
        info.setDefaultModelName("qwen-plus");
        Map<String, Object> sharedProperties = new LinkedHashMap<String, Object>();
        sharedProperties.put("temperature", 0.2);
        sharedProperties.put("maxTokens", 1024);
        info.setProperties(sharedProperties);
        ModelDataSourceInfo.ModelConfig qwen = new ModelDataSourceInfo.ModelConfig();
        Map<String, Object> qwenProperties = new LinkedHashMap<String, Object>();
        qwenProperties.put("temperature", 0.7);
        qwen.setProperties(qwenProperties);
        info.setModelConfigs(Collections.singletonMap("qwen-plus", qwen));

        dataSource.createClient(info);

        assertEquals("chat-default.gpt-4o-mini", factory.createdDefinitions.get(0).getKey());
        assertEquals("langchain4j", factory.createdDefinitions.get(0).getFramework());
        assertEquals("fake", factory.createdDefinitions.get(0).getProvider());
        assertEquals(AiModelType.CHAT, factory.createdDefinitions.get(0).getType());
        assertEquals("gpt-4o-mini", factory.createdDefinitions.get(0).getProperties().get("modelName"));
        assertEquals(0.2, factory.createdDefinitions.get(0).getProperties().get("temperature"));
        assertEquals(1024, factory.createdDefinitions.get(0).getProperties().get("maxTokens"));
        assertEquals("chat-default.qwen-plus", factory.createdDefinitions.get(1).getKey());
        assertEquals("langchain4j", factory.createdDefinitions.get(1).getFramework());
        assertEquals("fake", factory.createdDefinitions.get(1).getProvider());
        assertEquals(AiModelType.CHAT, factory.createdDefinitions.get(1).getType());
        assertEquals("qwen-plus", factory.createdDefinitions.get(1).getProperties().get("modelName"));
        assertEquals(0.7, factory.createdDefinitions.get(1).getProperties().get("temperature"));
        assertEquals(1024, factory.createdDefinitions.get(1).getProperties().get("maxTokens"));
        assertSame(factory.createdModels.get(1), dataSource.getModel("chat-default"));
        assertSame(factory.createdModels.get(0), dataSource.getModel("chat-default.gpt-4o-mini"));
        assertSame(factory.createdModels.get(1), dataSource.getModel("chat-default.qwen-plus"));
        assertTrue(dataSource.keys().contains("chat-default"));
        assertTrue(dataSource.keys().contains("chat-default.gpt-4o-mini"));
        assertTrue(dataSource.keys().contains("chat-default.qwen-plus"));
    }

    @Test
    void destroyClientByKeyRemovesRuntimeModelGroup() {
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("boot", model("boot"), "boot"), factory, Collections.<AiModelAdapter>emptyList());
        ModelDataSourceInfo info = info("chat-default");
        info.setModelName(Arrays.asList("gpt-4o-mini", "qwen-plus"));
        dataSource.createClient(info);

        dataSource.destroyClientByKey("chat-default");

        assertNull(dataSource.getModel("chat-default"));
        assertNull(dataSource.getModel("chat-default.gpt-4o-mini"));
        assertNull(dataSource.getModel("chat-default.qwen-plus"));
        assertTrue(dataSource.datasourceNodes().isEmpty());
    }

    @Test
    void disabledResourceRemovesExistingRuntimeModelWithoutCreatingReplacement() {
        AiModel bootModel = model("chat");
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("chat", bootModel, "chat"), factory, Collections.<AiModelAdapter>emptyList());
        dataSource.createClient(info("chat"));
        ModelDataSourceInfo disabled = info("chat");
        disabled.setEnabled(false);
        factory.createdDefinition = null;

        dataSource.createClient(disabled);

        assertModel(dataSource.getModel("chat"), "chat", "boot-framework", "boot-provider", AiModelType.CHAT);
        assertTrue(dataSource.datasourceNodes().isEmpty());
        assertNull(factory.createdDefinition);
    }

    @Test
    void datasourceNodesClearDirectAndPropertySensitiveFields() {
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("boot", model("boot"), "boot"), new RecordingFactory(), Collections.<AiModelAdapter>emptyList());
        ModelDataSourceInfo info = info("chat");
        info.setApiKey("api-key");
        info.setSecretKey("secret-key");
        info.setAccessToken("access-token");
        info.setToken("token");
        info.setPassword("password");
        Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("apiKey", "property-api-key");
        properties.put("secretKey", "property-secret-key");
        properties.put("accessToken", "property-access-token");
        properties.put("token", "property-token");
        properties.put("password", "property-password");
        properties.put("safe", "value");
        info.setProperties(properties);

        dataSource.createClient(info);

        ModelDataSourceInfo node = dataSource.datasourceNodes().get(0);
        assertNull(node.getApiKey());
        assertNull(node.getSecretKey());
        assertNull(node.getAccessToken());
        assertNull(node.getToken());
        assertNull(node.getPassword());
        assertNull(node.getProperties().get("apiKey"));
        assertNull(node.getProperties().get("secretKey"));
        assertNull(node.getProperties().get("accessToken"));
        assertNull(node.getProperties().get("token"));
        assertNull(node.getProperties().get("password"));
        assertEquals("value", node.getProperties().get("safe"));
    }

    @Test
    void datasourceNodesRecursivelyClearSensitiveProperties() {
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("boot", model("boot"), "boot"), new RecordingFactory(), Collections.<AiModelAdapter>emptyList());
        ModelDataSourceInfo info = info("chat");
        Map<String, Object> credentials = new LinkedHashMap<String, Object>();
        credentials.put("apiKey", "nested-api-key");
        credentials.put("region", "cn-north-1");
        Map<String, Object> nested = new LinkedHashMap<String, Object>();
        nested.put("credentials", credentials);
        nested.put("items", Arrays.<Object>asList(
                Collections.<String, Object>singletonMap("password", "nested-password"),
                Collections.<String, Object>singletonMap("safe", "kept")));
        nested.put("array", new Object[] {Collections.<String, Object>singletonMap("token", "nested-token"), "plain"});
        info.setProperties(nested);

        dataSource.createClient(info);

        Map<String, Object> sanitized = dataSource.datasourceNodes().get(0).getProperties();
        Map<?, ?> sanitizedCredentials = (Map<?, ?>) sanitized.get("credentials");
        List<?> sanitizedItems = (List<?>) sanitized.get("items");
        Object[] sanitizedArray = (Object[]) sanitized.get("array");
        assertNull(sanitizedCredentials.get("apiKey"));
        assertEquals("cn-north-1", sanitizedCredentials.get("region"));
        assertNull(((Map<?, ?>) sanitizedItems.get(0)).get("password"));
        assertEquals("kept", ((Map<?, ?>) sanitizedItems.get(1)).get("safe"));
        assertNull(((Map<?, ?>) sanitizedArray[0]).get("token"));
        assertEquals("plain", sanitizedArray[1]);
    }

    @Test
    void createClientPassesDirectCredentialsToFactoryDefinitionProperties() {
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("boot", model("boot"), "boot"), factory, Collections.<AiModelAdapter>emptyList());
        ModelDataSourceInfo info = info("chat");
        info.setApiKey("api-key");
        info.setSecretKey("secret-key");
        info.setAccessToken("access-token");
        info.setToken("token");
        info.setPassword("password");

        dataSource.createClient(info);

        assertEquals("api-key", factory.createdDefinition.getProperties().get("apiKey"));
        assertEquals("secret-key", factory.createdDefinition.getProperties().get("secretKey"));
        assertEquals("access-token", factory.createdDefinition.getProperties().get("accessToken"));
        assertEquals("token", factory.createdDefinition.getProperties().get("token"));
        assertEquals("password", factory.createdDefinition.getProperties().get("password"));
    }

    @Test
    void createClientAcceptsExplicitNullPropertiesWithoutPartialRuntimeState() {
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("boot", model("boot"), "boot"), factory, Collections.<AiModelAdapter>emptyList());
        ModelDataSourceInfo info = info("chat");
        info.setProperties(null);

        dataSource.createClient(info);

        assertModel(dataSource.getModel("chat"), "chat", "langchain4j", "fake", AiModelType.CHAT);
        assertEquals(1, dataSource.datasourceNodes().size());
        assertTrue(dataSource.datasourceNodes().get(0).getProperties().isEmpty());
    }

    @Test
    void createClientUsesModelConfigurationPropertiesInsteadOfDatasourceDefaults() {
        configure("magic.api.model.chat.baseUrl=http://configured.example/v1",
                "magic.api.model.chat.apiKey=configured-api-key",
                "magic.api.model.chat.properties={\"timeout\":\"PT10S\",\"custom\":\"configured\"}");
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("boot", model("boot"), "boot"), factory, Collections.<AiModelAdapter>emptyList());
        ModelDataSourceInfo info = info("chat");
        info.setBaseUrl("http://default.example/v1");
        info.setApiKey("default-api-key");
        info.setProperties(new LinkedHashMap<String, Object>() {{
            put("timeout", "PT30S");
            put("legacyOnly", "default-value");
        }});

        dataSource.createClient(info);

        assertEquals("http://configured.example/v1", factory.createdDefinition.getProperties().get("baseUrl"));
        assertEquals("configured-api-key", factory.createdDefinition.getProperties().get("apiKey"));
        assertEquals("PT10S", factory.createdDefinition.getProperties().get("timeout"));
        assertEquals("configured", factory.createdDefinition.getProperties().get("custom"));
        assertTrue(!factory.createdDefinition.getProperties().containsKey("legacyOnly"));
    }

    @Test
    void createClientRejectsInvalidDefinitions() {
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("boot", model("boot"), "boot"), new RecordingFactory(), Collections.<AiModelAdapter>emptyList());

        assertThrows(AiModelException.class, () -> dataSource.createClient(null));
        ModelDataSourceInfo blankKey = info(" ");
        assertThrows(AiModelException.class, () -> dataSource.createClient(blankKey));
        ModelDataSourceInfo blankProvider = info("chat");
        blankProvider.setProvider(" ");
        assertThrows(AiModelException.class, () -> dataSource.createClient(blankProvider));
        ModelDataSourceInfo nullModelType = info("chat");
        nullModelType.setModelType(null);
        assertThrows(AiModelException.class, () -> dataSource.createClient(nullModelType));
    }

    private ModelDataSourceInfo info(String key) {
        ModelDataSourceInfo info = new ModelDataSourceInfo();
        info.setKey(key);
        info.setProvider("fake");
        info.setModelType(LangChain4jModelType.CHAT);
        info.setEnabled(true);
        info.setModelName(Collections.singletonList("default-model"));
        return info;
    }

    private void configure(String... properties) {
        applicationContext = new GenericApplicationContext();
        TestPropertyValues.of(properties).applyTo(applicationContext);
        applicationContext.refresh();
        new SpringUtils().setApplicationContext(applicationContext);
    }

    private AiModel model(String key) {
        return new FakeAiModel(key, "boot-framework", "boot-provider", AiModelType.CHAT);
    }

    private AiModelRegistry registryWith(String key, AiModel model, String defaultKey) {
        Map<String, AiModel> models = new LinkedHashMap<String, AiModel>();
        models.put(key, model);
        return new TestRegistry(models, defaultKey);
    }

    private static void assertModel(AiModel model, String key, String framework, String provider, AiModelType type) {
        assertEquals(key, model.getKey());
        assertEquals(framework, model.getFramework());
        assertEquals(provider, model.getProvider());
        assertEquals(type, model.getType());
    }

    private static class RecordingFactory implements AiModelFactory {
        private final List<AiModel> createdModels = new java.util.ArrayList<AiModel>();
        private final List<AiModelDefinition> createdDefinitions = new java.util.ArrayList<AiModelDefinition>();
        private AiModelDefinition createdDefinition;

        @Override
        public AiModel create(AiModelDefinition definition, List<AiModelAdapter> adapters) {
            createdDefinition = definition;
            createdDefinitions.add(definition);
            AiModel model = new FakeAiModel(
                    definition.getKey(), definition.getFramework(), definition.getProvider(), definition.getType());
            createdModels.add(model);
            return model;
        }
    }

    private static class TestRegistry implements AiModelRegistry {
        private final Map<String, AiModel> models;
        private final String defaultKey;

        private TestRegistry(Map<String, AiModel> models, String defaultKey) {
            this.models = models;
            this.defaultKey = defaultKey;
        }

        @Override
        public AiModel get(String key) { return models.get(key); }

        @Override
        public AiModel getDefault() { return models.get(defaultKey); }

        @Override
        public String getDefaultKey() { return defaultKey; }

        @Override
        public boolean contains(String key) { return models.containsKey(key); }

        @Override
        public Set<String> keys() { return models.keySet(); }
    }

    private static class FakeAiModel implements AiModel {
        private final String key;
        private final String framework;
        private final String provider;
        private final AiModelType type;

        private FakeAiModel(String key, String framework, String provider, AiModelType type) {
            this.key = key;
            this.framework = framework;
            this.provider = provider;
            this.type = type;
        }

        @Override
        public String getKey() { return key; }

        @Override
        public String getFramework() { return framework; }

        @Override
        public String getProvider() { return provider; }

        @Override
        public AiModelType getType() { return type; }
    }
}
