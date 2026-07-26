package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.module.ModelModule;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
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
        LangChain4jModel chat = model("chat-native");
        ModelModule module = new ModelModule(dataSource("chat", chat, "chat"));

        ModelModule resolved = module.getDynamicAttribute("chat");

        assertSame(chat, resolved.getModelObject());
        assertSame(chat.getModel(), resolved.getModel());
    }

    @Test
    void dynamicModuleUsesDefaultDatasourceFromMagicContext() {
        LangChain4jModel chat = model("chat-native");
        ModelModule module = new ModelModule(dataSource("chat", chat, "chat"));
        MagicScriptContext context = new MagicScriptContext();
        context.set(Options.DEFAULT_DATA_SOURCE.getValue(), "chat");

        assertSame(chat, module.getDynamicModule(context).getModelObject());
    }

    private MagicDynamicModelDataSource dataSource(String key, LangChain4jModel model, String defaultKey) {
        final Map<String, LangChain4jModel> models = new LinkedHashMap<String, LangChain4jModel>();
        models.put(key, model);
        return new MagicDynamicModelDataSource(new LangChain4jModelRegistry() {
            @Override
            public LangChain4jModel get(String modelKey) {
                return models.get(modelKey);
            }

            @Override
            public LangChain4jModel getDefault() {
                return models.get(defaultKey);
            }

            @Override
            public boolean contains(String modelKey) {
                return models.containsKey(modelKey);
            }

            @Override
            public Set<String> keys() {
                return models.keySet();
            }
        }, new LangChain4jModelFactory() {
            @Override
            public LangChain4jModel create(com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition definition,
                                           java.util.List<LangChain4jModelProviderAdapter> adapters) {
                return null;
            }
        }, Collections.<LangChain4jModelProviderAdapter>emptyList());
    }

    private LangChain4jModel model(Object nativeModel) {
        return new LangChain4jModel("fake", LangChain4jModelType.CHAT, nativeModel);
    }
}
