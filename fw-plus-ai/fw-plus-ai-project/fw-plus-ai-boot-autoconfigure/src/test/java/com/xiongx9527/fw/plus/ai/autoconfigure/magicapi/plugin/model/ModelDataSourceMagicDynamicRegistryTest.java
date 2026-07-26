package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.ModelDataSourceInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.service.ModelDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.service.ModelDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelDataSourceMagicDynamicRegistryTest {

    @Test
    void storageUsesModelDatasourceFolder() {
        assertEquals("model-datasource", new ModelDataSourceInfoMagicResourceStorage().folder());
    }

    @Test
    void registerPersistedDataSourcesCreatesRuntimeModel() {
        ModelDataSourceInfo info = info("chat");
        ModelDataSourceInfoMagicResourceStorage storage = mock(ModelDataSourceInfoMagicResourceStorage.class);
        when(storage.buildKey(info)).thenReturn("chat-0");
        MagicDynamicModelDataSource dataSource = mock(MagicDynamicModelDataSource.class);

        new TestRegistry(storage, dataSource).registerRuntime(info);

        verify(dataSource).createClient(info);
    }

    @Test
    void updateSameKeyDestroysBeforeEachCreate() {
        ModelDataSourceInfo info = info("chat");
        ModelDataSourceInfoMagicResourceStorage storage = mock(ModelDataSourceInfoMagicResourceStorage.class);
        when(storage.buildKey(info)).thenReturn("chat-0");
        MagicDynamicModelDataSource dataSource = mock(MagicDynamicModelDataSource.class);

        TestRegistry registry = new TestRegistry(storage, dataSource);
        registry.registerRuntime(info);
        registry.registerRuntime(info);

        InOrder order = inOrder(dataSource);
        order.verify(dataSource).destroyClientByKey("chat");
        order.verify(dataSource).createClient(info);
        order.verify(dataSource).destroyClientByKey("chat");
        order.verify(dataSource).createClient(info);
    }

    @Test
    void failedMappingRegistrationRollsBackRuntimeModel() {
        ModelDataSourceInfo info = info("chat");
        ModelDataSourceInfoMagicResourceStorage storage = mock(ModelDataSourceInfoMagicResourceStorage.class);
        when(storage.buildKey(info)).thenReturn("chat-0");
        MagicDynamicModelDataSource dataSource = mock(MagicDynamicModelDataSource.class);
        TestRegistry registry = new TestRegistry(storage, dataSource);
        registry.failMappingRegistration = true;

        assertFalse(registry.registerRuntime(info));

        InOrder order = inOrder(dataSource);
        order.verify(dataSource).destroyClientByKey("chat");
        order.verify(dataSource).createClient(info);
        order.verify(dataSource).destroyClientByKey("chat");
    }

    @Test
    void unregisterRemovesRuntimeModel() {
        ModelDataSourceInfo info = info("chat");
        ModelDataSourceInfoMagicResourceStorage storage = mock(ModelDataSourceInfoMagicResourceStorage.class);
        MagicDynamicModelDataSource dataSource = mock(MagicDynamicModelDataSource.class);

        new TestRegistry(storage, dataSource).unregisterRuntime(info);

        verify(dataSource).destroyClientByKey("chat");
    }

    private ModelDataSourceInfo info(String key) {
        ModelDataSourceInfo info = new ModelDataSourceInfo();
        info.setKey(key);
        info.setProvider("fake");
        info.setModelType(LangChain4jModelType.CHAT);
        return info;
    }

    private static class TestRegistry extends ModelDataSourceMagicDynamicRegistry {
        private boolean failMappingRegistration;
        private TestRegistry(ModelDataSourceInfoMagicResourceStorage storage, MagicDynamicModelDataSource dataSource) {
            super(storage, dataSource);
        }

        private boolean registerRuntime(ModelDataSourceInfo info) {
            return register(buildMappingNode(info));
        }

        private void unregisterRuntime(ModelDataSourceInfo info) {
            unregister(buildMappingNode(info));
        }

        @Override
        protected boolean registerMapping(MappingNode<ModelDataSourceInfo> mappingNode) {
            return !failMappingRegistration && super.registerMapping(mappingNode);
        }
    }
}
