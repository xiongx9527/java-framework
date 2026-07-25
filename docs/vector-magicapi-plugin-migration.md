# Magic-API Vector Plugin Migration

The `milvus`, `qdrant`, `redisstack`, and `weaviate` Magic-API entries are deprecated compatibility wrappers. Keep vector store configuration in the Core embedding-store registry and use the `vector` module in scripts.

## Configuration

```yaml
fw-plus:
  ai:
    langchain4j:
      embedding-store:
        enabled: true
        default-store: knowledge
        stores:
          knowledge:
            provider: milvus
            uri: http://127.0.0.1:19530
            collection-name: knowledge
```

## Script Migration

Use the unified module and an embedding-store key:

```javascript
import vector;

var store = vector['knowledge'].getOperations().getEmbeddingStore();
```

| Deprecated import | Replacement |
| --- | --- |
| `import milvus` | `import vector; vector['key']` |
| `import qdrant` | `import vector; vector['key']` |
| `import redisstack` | `import vector; vector['key']` |
| `import weaviate` | `import vector; vector['key']` |

The legacy modules resolve the requested key through `EmbeddingStoreRegistry`; they no longer construct provider-specific LangChain4j stores. Their datasource-management controllers and resource storage remain available during the transition, but do not configure embedding stores.
