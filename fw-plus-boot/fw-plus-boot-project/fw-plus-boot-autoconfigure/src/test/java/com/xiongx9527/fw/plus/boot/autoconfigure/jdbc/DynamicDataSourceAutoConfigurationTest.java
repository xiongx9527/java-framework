package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc;

import com.baomidou.dynamic.datasource.creator.DataSourceProperty;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.properties.DefaultDataSourceProperty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class DynamicDataSourceAutoConfigurationTest {

    @Test
    void constructorDoesNotRegisterDefaultDatasourceWhenUrlIsBlank() {
        DynamicDataSourceProperties properties = new DynamicDataSourceProperties();
        DefaultDataSourceProperty defaultProperty = new DefaultDataSourceProperty();
        defaultProperty.setUrl("   ");

        new DynamicDataSourceAutoConfiguration(properties, defaultProperty, emptyCustomizerProvider());

        assertTrue(properties.getDatasource() == null || properties.getDatasource().isEmpty());
    }

    @Test
    void constructorRegistersDefaultDatasourceUnderMasterWhenPrimaryIsBlank() {
        DynamicDataSourceProperties properties = new DynamicDataSourceProperties();
        properties.setPrimary(" ");
        DefaultDataSourceProperty defaultProperty = new DefaultDataSourceProperty();
        defaultProperty.setUrl("jdbc:h2:mem:test");

        new DynamicDataSourceAutoConfiguration(properties, defaultProperty, emptyCustomizerProvider());

        assertSame(defaultProperty, properties.getDatasource().get("master"));
    }

    @Test
    void constructorDoesNotOverrideExistingPrimaryDatasource() {
        DynamicDataSourceProperties properties = new DynamicDataSourceProperties();
        properties.setPrimary("master");
        DataSourceProperty existing = new DataSourceProperty();
        LinkedHashMap<String, DataSourceProperty> datasource = new LinkedHashMap<>();
        datasource.put("master", existing);
        properties.setDatasource(datasource);
        DefaultDataSourceProperty defaultProperty = new DefaultDataSourceProperty();
        defaultProperty.setUrl("jdbc:h2:mem:test");

        new DynamicDataSourceAutoConfiguration(properties, defaultProperty, emptyCustomizerProvider());

        assertSame(existing, properties.getDatasource().get("master"));
    }

    private ObjectProvider emptyCustomizerProvider() {
        return new ObjectProvider() {
            @Override
            public Object getObject(Object... args) {
                return Collections.emptyList();
            }

            @Override
            public Object getIfAvailable() {
                return Collections.emptyList();
            }

            @Override
            public Object getIfUnique() {
                return Collections.emptyList();
            }

            @Override
            public Object getObject() {
                return Collections.emptyList();
            }
        };
    }
}
