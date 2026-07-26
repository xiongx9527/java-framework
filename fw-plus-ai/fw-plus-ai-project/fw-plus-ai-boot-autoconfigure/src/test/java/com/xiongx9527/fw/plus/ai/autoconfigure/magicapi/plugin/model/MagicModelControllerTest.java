package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.ModelDataSourceInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.web.MagicModelController;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class MagicModelControllerTest {

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
    void datasourceListMasksCredentials() {
        RecordingFactory factory = new RecordingFactory(false);
        MagicDynamicModelDataSource dataSource = new MagicDynamicModelDataSource(null, factory,
                Collections.<LangChain4jModelProviderAdapter>emptyList());
        dataSource.createClient(definition());
        MagicModelController controller = new MagicModelController(new MagicConfiguration(), dataSource, factory,
                Collections.<LangChain4jModelProviderAdapter>emptyList());

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

    private MagicModelController controller(LangChain4jModelFactory factory) {
        return new MagicModelController(new MagicConfiguration(), factory,
                Collections.<LangChain4jModelProviderAdapter>emptyList());
    }

    private ModelDataSourceInfo definition() {
        ModelDataSourceInfo definition = new ModelDataSourceInfo();
        definition.setKey("chat");
        definition.setProvider("fake");
        definition.setModelType(LangChain4jModelType.CHAT);
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

    private static class RecordingFactory implements LangChain4jModelFactory {
        private final boolean fail;
        private LangChain4jModelDefinition definition;

        private RecordingFactory(boolean fail) {
            this.fail = fail;
        }

        @Override
        public LangChain4jModel create(LangChain4jModelDefinition input, List<LangChain4jModelProviderAdapter> adapters) {
            definition = input;
            if (fail) {
                throw new IllegalStateException("failed with " + input.getApiKey() + " and " + input.getProperties().get("secretKey"));
            }
            return new LangChain4jModel("fake", LangChain4jModelType.CHAT, "native");
        }
    }
}
