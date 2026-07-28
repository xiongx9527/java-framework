package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.web;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
public class MagicVectorControllerTest {

    @Test
    public void controllerContractDoesNotExposeLangChain4jTypes() {
        for (Field field : MagicVectorController.class.getDeclaredFields()) {
            assertFalse(field.getGenericType().getTypeName().contains("langchain4j"));
        }
        for (Constructor<?> constructor : MagicVectorController.class.getDeclaredConstructors()) {
            assertFalse(constructor.toGenericString().contains("langchain4j"));
        }
    }

    @Test
    public void datasourceTestShouldRejectMissingProvider() {
        AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
        definition.setKey("knowledge");

        JsonBean<Object> result = controller().test(definition);

        assertEquals(-1, result.getCode());
        assertTrue(result.getMessage().contains("provider"));
    }

    @Test
    public void datasourceTestShouldRejectMissingKey() {
        AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
        definition.setProvider("fake");

        JsonBean<Object> result = controller().test(definition);

        assertEquals(-1, result.getCode());
        assertTrue(result.getMessage().contains("key"));
    }

    @Test
    public void datasourceTestShouldReturnOkWhenProviderCreatesStore() {
        AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
        definition.setKey("knowledge");
        definition.setFramework("langchain4j");
        definition.setProvider("fake");
        definition.setType(AiEmbeddingStoreType.VECTOR);

        JsonBean<Object> result = controller().test(definition);

        assertEquals(1, result.getCode());
        assertEquals("ok", result.getData());
    }

    @Test
    public void datasourceTestShouldNotExposeCredentialsWhenFactoryFails() {
        AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
        definition.setKey("knowledge");
        definition.setFramework("langchain4j");
        definition.setProvider("fake");
        definition.setType(AiEmbeddingStoreType.VECTOR);
        java.util.Map<String, Object> properties = new LinkedHashMap<String, Object>();
        properties.put("apiKey", "vector-secret");
        definition.setProperties(properties);
        MagicVectorController controller = new MagicVectorController(new MagicConfiguration(),
                new AiEmbeddingStoreFactory() {
                    @Override
                    public AiEmbeddingStore create(AiEmbeddingStoreDefinition input,
                                                   java.util.List<AiEmbeddingStoreAdapter> adapters) {
                        throw new IllegalStateException("failed with " + input.getProperties().get("apiKey"));
                    }
                }, Collections.<AiEmbeddingStoreAdapter>emptyList());

        JsonBean<Object> result = controller.test(definition);

        assertEquals(-1, result.getCode());
        assertFalse(String.valueOf(result).contains("vector-secret"));
    }

    @Test
    public void datasourceTestShouldMaskIllegalArgumentExceptionFromFactory(CapturedOutput output) {
        final String sensitiveMessage = "apiKey=api-secret, token=token-secret, password=password-secret";
        AiEmbeddingStoreDefinition definition = new AiEmbeddingStoreDefinition();
        definition.setKey("knowledge");
        definition.setFramework("langchain4j");
        definition.setProvider("fake");
        definition.setType(AiEmbeddingStoreType.VECTOR);
        MagicVectorController controller = new MagicVectorController(new MagicConfiguration(),
                new AiEmbeddingStoreFactory() {
                    @Override
                    public AiEmbeddingStore create(AiEmbeddingStoreDefinition input,
                                                   java.util.List<AiEmbeddingStoreAdapter> adapters) {
                        throw new IllegalArgumentException(sensitiveMessage);
                    }
                }, Collections.<AiEmbeddingStoreAdapter>emptyList());

        JsonBean<Object> result = controller.test(definition);

        assertEquals(-1, result.getCode());
        assertFalse(String.valueOf(result).contains(sensitiveMessage));
        assertFalse(output.getAll().contains(sensitiveMessage));
    }

    private MagicVectorController controller() {
        return new MagicVectorController(new MagicConfiguration(), new AiEmbeddingStoreFactory() {
            @Override
            public AiEmbeddingStore create(AiEmbeddingStoreDefinition definition,
                                           java.util.List<AiEmbeddingStoreAdapter> adapters) {
                return mock(AiEmbeddingStore.class);
            }
        }, Collections.<AiEmbeddingStoreAdapter>emptyList());
    }
}
