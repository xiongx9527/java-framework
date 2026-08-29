package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.lang.reflect.Method;

public class OracleEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

    private static final String STORE_CLASS = "dev.langchain4j.store.embedding.oracle.OracleEmbeddingStore";

    private static final String CREATE_OPTION_CLASS = "dev.langchain4j.store.embedding.oracle.CreateOption";

    private final ApplicationContext applicationContext;

    public OracleEmbeddingStoreProviderAdapter() {
        this(null);
    }

    public OracleEmbeddingStoreProviderAdapter(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean supports(String provider) {
        return ProviderAdapterSupport.supports(provider, "oracle");
    }

    @Override
    @SuppressWarnings("unchecked")
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            Object builder = builder();
            invoke(builder, "dataSource", new Class<?>[]{DataSource.class},
                    new Object[]{ProviderAdapterSupport.requiredBeanProperty(definition, "dataSourceBean",
                            DataSource.class, applicationContext)});
            String embeddingTable = ProviderAdapterSupport.requiredStringProperty(definition, "embeddingTable");
            Object embeddingTableCreateOption = createOption(definition, "embeddingTableCreateOption");
            if (embeddingTableCreateOption == null) {
                invoke(builder, "embeddingTable", new Class<?>[]{String.class}, new Object[]{embeddingTable});
            } else {
                Class<?> createOptionClass = Class.forName(CREATE_OPTION_CLASS);
                invoke(builder, "embeddingTable", new Class<?>[]{String.class, createOptionClass},
                        new Object[]{embeddingTable, embeddingTableCreateOption});
            }
            Object vectorIndex = createOption(definition, "vectorIndex");
            if (vectorIndex != null) {
                invoke(builder, "vectorIndex", new Class<?>[]{Class.forName(CREATE_OPTION_CLASS)}, new Object[]{vectorIndex});
            }
            Boolean exactSearch = ProviderAdapterSupport.booleanProperty(definition, "exactSearch");
            if (exactSearch != null) {
                invoke(builder, "exactSearch", new Class<?>[]{boolean.class}, new Object[]{exactSearch});
            }
            return (EmbeddingStore<TextSegment>) invoke(builder, "build", new Class<?>[0], new Object[0]);
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }

    private Object builder() throws Exception {
        return Class.forName(STORE_CLASS).getMethod("builder").invoke(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object createOption(EmbeddingStoreDefinition definition, String name) throws Exception {
        String value = ProviderAdapterSupport.stringProperty(definition, name);
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        Class<?> createOptionClass = Class.forName(CREATE_OPTION_CLASS);
        return Enum.valueOf((Class<Enum>) createOptionClass, value.trim().replace('-', '_').toUpperCase());
    }

    private Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object[] args) throws Exception {
        Method method = target.getClass().getMethod(methodName, parameterTypes);
        return method.invoke(target, args);
    }
}
