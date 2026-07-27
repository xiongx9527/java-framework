package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.ModelDataSourceInfo;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelException;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import org.junit.jupiter.api.Test;

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

    @Test
    void runtimeModelOverridesSpringBootModelAndFallsBackAfterDestroy() {
        LangChain4jModel bootModel = model("boot");
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("chat", bootModel, "chat"), factory, Collections.<LangChain4jModelProviderAdapter>emptyList());

        dataSource.createClient(info("chat"));

        assertSame(factory.createdModel, dataSource.getModel("chat"));
        dataSource.destroyClientByKey("chat");
        assertSame(bootModel, dataSource.getModel("chat"));
    }

    @Test
    void runtimeModelOverridesSpringBootDefaultModel() {
        LangChain4jModel bootModel = model("boot");
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("chat", bootModel, "chat"), factory, Collections.<LangChain4jModelProviderAdapter>emptyList());

        dataSource.createClient(info("chat"));

        assertSame(factory.createdModel, dataSource.getDefault());
    }

    @Test
    void runtimeModelOverridesSpringBootDefaultGroupAlias() {
        LangChain4jModel bootModel = model("boot");
        Map<String, LangChain4jModel> models = new LinkedHashMap<String, LangChain4jModel>();
        models.put("chat-default", bootModel);
        models.put("chat-default.openai", bootModel);
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                new TestRegistry(models, "chat-default"), factory, Collections.<LangChain4jModelProviderAdapter>emptyList());

        dataSource.createClient(info("chat-default"));

        assertSame(factory.createdModel, dataSource.getDefault());
    }

    @Test
    void runtimeGroupAliasDoesNotOverrideExplicitDefaultModelNameKey() {
        LangChain4jModel bootDefaultModel = model("boot-default");
        LangChain4jModel bootAliasModel = model("boot-alias");
        Map<String, LangChain4jModel> models = new LinkedHashMap<String, LangChain4jModel>();
        models.put("chat-default", bootAliasModel);
        models.put("chat-default.qwen-plus", bootDefaultModel);
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                new TestRegistry(models, "chat-default.qwen-plus"), factory, Collections.<LangChain4jModelProviderAdapter>emptyList());

        dataSource.createClient(info("chat-default"));

        assertSame(bootDefaultModel, dataSource.getDefault());
    }

    @Test
    void createClientRegistersMultipleRuntimeModelsWithDefaultAlias() {
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("boot", model("boot"), "boot"), factory, Collections.<LangChain4jModelProviderAdapter>emptyList());
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
        assertEquals("gpt-4o-mini", factory.createdDefinitions.get(0).getModelName());
        assertEquals(0.2, factory.createdDefinitions.get(0).getProperties().get("temperature"));
        assertEquals(1024, factory.createdDefinitions.get(0).getProperties().get("maxTokens"));
        assertEquals("chat-default.qwen-plus", factory.createdDefinitions.get(1).getKey());
        assertEquals("qwen-plus", factory.createdDefinitions.get(1).getModelName());
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
                registryWith("boot", model("boot"), "boot"), factory, Collections.<LangChain4jModelProviderAdapter>emptyList());
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
        LangChain4jModel bootModel = model("boot");
        RecordingFactory factory = new RecordingFactory();
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("chat", bootModel, "chat"), factory, Collections.<LangChain4jModelProviderAdapter>emptyList());
        dataSource.createClient(info("chat"));
        ModelDataSourceInfo disabled = info("chat");
        disabled.setEnabled(false);
        factory.createdDefinition = null;

        dataSource.createClient(disabled);

        assertSame(bootModel, dataSource.getModel("chat"));
        assertTrue(dataSource.datasourceNodes().isEmpty());
        assertNull(factory.createdDefinition);
    }

    @Test
    void datasourceNodesClearDirectAndPropertySensitiveFields() {
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("boot", model("boot"), "boot"), new RecordingFactory(), Collections.<LangChain4jModelProviderAdapter>emptyList());
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
                registryWith("boot", model("boot"), "boot"), new RecordingFactory(), Collections.<LangChain4jModelProviderAdapter>emptyList());
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
                registryWith("boot", model("boot"), "boot"), factory, Collections.<LangChain4jModelProviderAdapter>emptyList());
        ModelDataSourceInfo info = info("chat");
        info.setApiKey("api-key");
        info.setSecretKey("secret-key");
        info.setAccessToken("access-token");
        info.setToken("token");
        info.setPassword("password");

        dataSource.createClient(info);

        assertEquals("api-key", factory.createdDefinition.getApiKey());
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
                registryWith("boot", model("boot"), "boot"), factory, Collections.<LangChain4jModelProviderAdapter>emptyList());
        ModelDataSourceInfo info = info("chat");
        info.setProperties(null);

        dataSource.createClient(info);

        assertSame(factory.createdModel, dataSource.getModel("chat"));
        assertEquals(1, dataSource.datasourceNodes().size());
        assertTrue(dataSource.datasourceNodes().get(0).getProperties().isEmpty());
    }

    @Test
    void createClientRejectsInvalidDefinitions() {
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(
                registryWith("boot", model("boot"), "boot"), new RecordingFactory(), Collections.<LangChain4jModelProviderAdapter>emptyList());

        assertThrows(LangChain4jModelException.class, () -> dataSource.createClient(null));
        ModelDataSourceInfo blankKey = info(" ");
        assertThrows(LangChain4jModelException.class, () -> dataSource.createClient(blankKey));
        ModelDataSourceInfo blankProvider = info("chat");
        blankProvider.setProvider(" ");
        assertThrows(LangChain4jModelException.class, () -> dataSource.createClient(blankProvider));
        ModelDataSourceInfo nullModelType = info("chat");
        nullModelType.setModelType(null);
        assertThrows(LangChain4jModelException.class, () -> dataSource.createClient(nullModelType));
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

    private LangChain4jModel model(String value) {
        return new LangChain4jModel("fake", LangChain4jModelType.CHAT, value);
    }

    private LangChain4jModelRegistry registryWith(String key, LangChain4jModel model, String defaultKey) {
        Map<String, LangChain4jModel> models = new LinkedHashMap<String, LangChain4jModel>();
        models.put(key, model);
        return new TestRegistry(models, defaultKey);
    }

    private static class RecordingFactory implements LangChain4jModelFactory {
        private final LangChain4jModel createdModel = new LangChain4jModel("fake", LangChain4jModelType.CHAT, "runtime");
        private final List<LangChain4jModel> createdModels = new java.util.ArrayList<LangChain4jModel>();
        private final List<LangChain4jModelDefinition> createdDefinitions = new java.util.ArrayList<LangChain4jModelDefinition>();
        private LangChain4jModelDefinition createdDefinition;

        @Override
        public LangChain4jModel create(LangChain4jModelDefinition definition, List<LangChain4jModelProviderAdapter> adapters) {
            createdDefinition = definition;
            createdDefinitions.add(definition);
            LangChain4jModel model = createdDefinitions.size() == 1
                    ? createdModel
                    : new LangChain4jModel("fake", definition.getModelType(), definition.getKey());
            createdModels.add(model);
            return model;
        }
    }

    private static class TestRegistry implements LangChain4jModelRegistry {
        private final Map<String, LangChain4jModel> models;
        private final String defaultKey;

        private TestRegistry(Map<String, LangChain4jModel> models, String defaultKey) {
            this.models = models;
            this.defaultKey = defaultKey;
        }

        @Override
        public LangChain4jModel get(String key) { return models.get(key); }

        @Override
        public LangChain4jModel getDefault() { return models.get(defaultKey); }

        @Override
        public String getDefaultKey() { return defaultKey; }

        @Override
        public boolean contains(String key) { return models.containsKey(key); }

        @Override
        public Set<String> keys() { return models.keySet(); }
    }
}
