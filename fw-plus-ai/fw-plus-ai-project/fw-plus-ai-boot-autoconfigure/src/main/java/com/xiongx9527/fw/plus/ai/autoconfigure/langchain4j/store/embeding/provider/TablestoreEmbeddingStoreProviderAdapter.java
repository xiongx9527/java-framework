package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.EmbeddingStoreDefinition;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

public class TablestoreEmbeddingStoreProviderAdapter extends ReflectiveEmbeddingStoreProviderAdapter {

    private static final String CONSTRUCTOR_ARGS = "constructorArgs";

    public TablestoreEmbeddingStoreProviderAdapter() {
        this(null);
    }

    public TablestoreEmbeddingStoreProviderAdapter(ApplicationContext applicationContext) {
        super("dev.langchain4j.store.embedding.tablestore.TablestoreEmbeddingStore", applicationContext, "tablestore");
    }

    @Override
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            EmbeddingStoreDefinition mapped = copyDefinition(definition);
            if (!mapped.getProperties().containsKey(CONSTRUCTOR_ARGS)) {
                mapped.getProperties().put(CONSTRUCTOR_ARGS, constructorArgs(mapped));
            }
            return super.create(mapped);
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }

    private List<Object> constructorArgs(EmbeddingStoreDefinition definition) {
        Object client = requiredProperty(definition, "clientBean");
        Integer dimension = ProviderAdapterSupport.requiredIntegerProperty(definition, "dimension");
        String tableName = ProviderAdapterSupport.stringProperty(definition, "tableName");
        List<Object> args = new ArrayList<Object>();
        args.add(client);
        if (tableName == null || tableName.trim().length() == 0) {
            args.add(dimension);
            Object metadataSchemaList = metadataSchemaList(definition);
            if (metadataSchemaList != null) {
                args.add(metadataSchemaList);
            }
            return args;
        }
        args.add(tableName);
        args.add(ProviderAdapterSupport.requiredStringProperty(definition, "searchIndexName"));
        args.add(ProviderAdapterSupport.requiredStringProperty(definition, "pkName"));
        args.add(ProviderAdapterSupport.requiredStringProperty(definition, "textField"));
        args.add(ProviderAdapterSupport.requiredStringProperty(definition, "embeddingField"));
        args.add(dimension);
        args.add(ProviderAdapterSupport.stringProperty(definition, "vectorMetricType"));
        args.add(metadataSchemaList(definition));
        return args;
    }

    private Object metadataSchemaList(EmbeddingStoreDefinition definition) {
        Object value = definition.getProperties().get("metadataSchemaList");
        return value == null ? definition.getProperties().get("metadataSchemaListBean") : value;
    }

    private Object requiredProperty(EmbeddingStoreDefinition definition, String name) {
        Object value = definition.getProperties().get(name);
        if (value == null || (value instanceof String && ((String) value).trim().length() == 0)) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
