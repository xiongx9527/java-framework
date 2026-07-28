package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.ModelDataSourceInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.web.MagicModelController;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelFactory;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicModelControllerTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void controllerContractDoesNotExposeLangChain4jTypes() {
        for (Field field : MagicModelController.class.getDeclaredFields()) {
            assertFalse(field.getGenericType().getTypeName().contains("langchain4j"));
        }
        for (Constructor<?> constructor : MagicModelController.class.getDeclaredConstructors()) {
            assertFalse(constructor.toGenericString().contains("langchain4j"));
        }
    }

    @Test
    void datasourceTestCallsFactoryAndDoesNotExposeCredentialsOnSuccess() {
        RecordingFactory factory = new RecordingFactory(false);
        JsonBean<Object> result = controller(factory).test(definition());

        assertNotNull(factory.definition);
        assertEquals(1, result.getCode());
        assertEquals("ok", result.getData());
        assertNoCredentials(String.valueOf(result));
    }

    @Test
    void datasourceTestMasksCredentialsWhenFactoryFails() {
        JsonBean<Object> result = controller(new RecordingFactory(true)).test(definition());

        assertEquals(-1, result.getCode());
        assertNoCredentials(String.valueOf(result));
    }

    @Test
    void datasourceTestCreatesAllModelNameDefinitions() {
        RecordingFactory factory = new RecordingFactory(false);
        ModelDataSourceInfo definition = definition();
        definition.setKey("chat-default");
        definition.setModelName(Arrays.asList("gpt-4o-mini", "qwen-plus"));

        JsonBean<Object> result = controller(factory).test(definition);

        assertEquals(1, result.getCode());
        assertEquals(2, factory.definitions.size());
        assertEquals("chat-default.gpt-4o-mini", factory.definitions.get(0).getKey());
        assertEquals("gpt-4o-mini", factory.definitions.get(0).getProperties().get("modelName"));
        assertEquals("chat-default.qwen-plus", factory.definitions.get(1).getKey());
        assertEquals("qwen-plus", factory.definitions.get(1).getProperties().get("modelName"));
    }

    @Test
    void datasourceJsonRejectsLegacySingleModelNameString() {
        String legacyJson = "{"
                + "\"key\":\"chat\","
                + "\"provider\":\"fake\","
                + "\"modelType\":\"CHAT\","
                + "\"modelName\":\"gpt-4o-mini\""
                + "}";

        assertThrows(Exception.class, () -> JSON.readValue(legacyJson, ModelDataSourceInfo.class));
    }

    @Test
    void datasourceTestRejectsInvalidDefaultModelName() {
        RecordingFactory factory = new RecordingFactory(false);
        ModelDataSourceInfo definition = definition();
        definition.setModelName(Arrays.asList("gpt-4o-mini", "qwen-plus"));
        definition.setDefaultModelName("missing");

        JsonBean<Object> result = controller(factory).test(definition);

        assertEquals(-1, result.getCode());
        assertTrue(factory.definitions.isEmpty());
    }

    @Test
    void datasourceListMasksCredentials() {
        RecordingFactory factory = new RecordingFactory(false);
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(null, factory,
                Collections.<AiModelAdapter>emptyList());
        dataSource.createClient(definition());
        MagicModelController controller = new MagicModelController(new MagicConfiguration(), dataSource, factory,
                Collections.<AiModelAdapter>emptyList());

        JsonBean<List<ModelDataSourceInfo>> result = controller.list();
        @SuppressWarnings("unchecked")
        List<ModelDataSourceInfo> models = (List<ModelDataSourceInfo>) result.getData();
        ModelDataSourceInfo info = models.get(0);

        assertNull(info.getApiKey());
        assertNull(info.getSecretKey());
        assertNull(info.getAccessToken());
        assertNull(info.getToken());
        assertNull(info.getPassword());
    }

    private MagicModelController controller(AiModelFactory factory) {
        return new MagicModelController(new MagicConfiguration(), factory,
                Collections.<AiModelAdapter>emptyList());
    }

    private ModelDataSourceInfo definition() {
        ModelDataSourceInfo definition = new ModelDataSourceInfo();
        definition.setKey("chat");
        definition.setProvider("fake");
        definition.setModelType(LangChain4jModelType.CHAT);
        definition.setModelName(Collections.singletonList("gpt-4o-mini"));
        definition.setApiKey("api-key-secret");
        definition.setSecretKey("secret-key-secret");
        definition.setAccessToken("access-token-secret");
        definition.setToken("token-secret");
        definition.setPassword("password-secret");
        return definition;
    }

    private void assertNoCredentials(String response) {
        assertFalse(response.contains("api-key-secret"));
        assertFalse(response.contains("secret-key-secret"));
        assertFalse(response.contains("access-token-secret"));
        assertFalse(response.contains("token-secret"));
        assertFalse(response.contains("password-secret"));
    }

    private static class RecordingFactory implements AiModelFactory {
        private final boolean fail;
        private final List<AiModelDefinition> definitions = new ArrayList<AiModelDefinition>();
        private AiModelDefinition definition;

        private RecordingFactory(boolean fail) {
            this.fail = fail;
        }

        @Override
        public AiModel create(AiModelDefinition input, List<AiModelAdapter> adapters) {
            definition = input;
            definitions.add(input);
            if (fail) {
                throw new IllegalStateException("failed with " + input.getProperties().get("apiKey")
                        + " and " + input.getProperties().get("secretKey"));
            }
            return new AiModel() {
                @Override public String getKey() { return input.getKey(); }
                @Override public String getFramework() { return input.getFramework(); }
                @Override public String getProvider() { return input.getProvider(); }
                @Override public AiModelType getType() { return input.getType(); }
            };
        }
    }
}
