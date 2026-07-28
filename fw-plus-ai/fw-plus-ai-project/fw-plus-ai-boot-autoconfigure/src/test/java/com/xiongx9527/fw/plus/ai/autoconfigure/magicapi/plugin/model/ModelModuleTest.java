package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.module.ModelModule;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelFactory;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.script.MagicScriptContext;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ModelModuleTest {

    @Test
    void usesModelMagicModuleName() {
        assertEquals("model", ModelModule.class.getAnnotation(MagicModule.class).value());
    }

    @Test
    void dynamicAttributeReturnsWrappedModelForExplicitKey() {
        AiModel chat = model("chat");
        ModelModule module = new ModelModule(dataSource("chat", chat, "chat"));

        ModelModule resolved = module.getDynamicAttribute("chat");

        assertSame(chat, resolved.getModelObject());
        assertSame(chat, resolved.getModel());
        assertModel(resolved.getModel(), "chat");
    }

    @Test
    void dynamicModuleUsesDefaultDatasourceFromMagicContext() {
        AiModel chat = model("chat");
        ModelModule module = new ModelModule(dataSource("chat", chat, "chat"));
        MagicScriptContext context = new MagicScriptContext();
        context.set(Options.DEFAULT_DATA_SOURCE.getValue(), "chat");

        AiModel resolved = module.getDynamicModule(context).getModelObject();
        assertSame(chat, resolved);
        assertModel(resolved, "chat");
    }

    private MagicDynamicModelDataSource dataSource(String key, AiModel model, String defaultKey) {
        final Map<String, AiModel> models = new LinkedHashMap<String, AiModel>();
        models.put(key, model);
        return new MagicDynamicModelDataSource(new AiModelRegistry() {
            @Override
            public AiModel get(String modelKey) {
                return models.get(modelKey);
            }

            @Override
            public AiModel getDefault() {
                return models.get(defaultKey);
            }

            @Override
            public String getDefaultKey() {
                return defaultKey;
            }

            @Override
            public boolean contains(String modelKey) {
                return models.containsKey(modelKey);
            }

            @Override
            public Set<String> keys() {
                return models.keySet();
            }
        }, new AiModelFactory() {
            @Override
            public AiModel create(AiModelDefinition definition, java.util.List<AiModelAdapter> adapters) {
                return null;
            }
        }, Collections.<AiModelAdapter>emptyList());
    }

    private AiModel model(final String key) {
        return new AiModel() {
            @Override
            public String getKey() { return key; }

            @Override
            public String getFramework() { return "fake-framework"; }

            @Override
            public String getProvider() { return "fake-provider"; }

            @Override
            public AiModelType getType() { return AiModelType.CHAT; }
        };
    }

    private void assertModel(AiModel model, String key) {
        assertEquals(key, model.getKey());
        assertEquals("fake-framework", model.getFramework());
        assertEquals("fake-provider", model.getProvider());
        assertEquals(AiModelType.CHAT, model.getType());
    }
}
