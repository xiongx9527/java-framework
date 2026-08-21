# Magic-API Vector Plugin Migration

The `milvus`, `qdrant`, `redisstack`, and `weaviate` Magic-API entries are deprecated compatibility wrappers. Configure stores in the Core embedding-store registry and use the `vector` module in scripts.

## Configuration

`key`, `provider`, `dimension`, and `metadata-keys` are store definition fields. Provider-specific values must be nested under `properties`; do not put `uri` or `collection-name` directly under a store.
Use the neutral `fw-plus.ai.embedding-store` prefix, set `framework: langchain4j`, and keep the store `type` as `VECTOR`; Qdrant, Milvus, Redis, and Weaviate are providers, not embedding-store types.

```yaml
fw-plus:
  ai:
    embedding-store:
      enabled: true
      default-store: knowledge-milvus
      stores:
        knowledge-milvus:
          framework: langchain4j
          provider: milvus
          type: VECTOR
          dimension: 768
          properties:
            uri: http://127.0.0.1:19530
            collectionName: knowledge
            apiKey: milvus-api-key
            indexType: HNSW
            metricType: COSINE
            consistencyLevel: EVENTUALLY
            retrieveEmbeddingsOnSearch: false
            autoFlushOnInsert: false
        archive-milvus:
          framework: langchain4j
          provider: milvus
          type: VECTOR
          dimension: 768
          properties:
            uri: http://127.0.0.1:19530
            collectionName: archive
            databaseName: archive_db
        qdrant-main:
          framework: langchain4j
          provider: qdrant
          type: VECTOR
          properties:
            host: 127.0.0.1
            port: 6334
            collectionName: knowledge
            payloadTextKey: content
            useTls: false
        redis-cache:
          framework: langchain4j
          provider: redis
          type: VECTOR
          dimension: 768
          metadata-keys:
            - tenantId
          properties:
            host: 127.0.0.1
            port: 6379
            indexName: knowledge_index
        weaviate-knowledge:
          framework: langchain4j
          provider: weaviate
          type: VECTOR
          properties:
            scheme: http
            host: 127.0.0.1
            port: 8080
            className: Knowledge
            avoidDups: true
            consistencyLevel: ALL
            textFieldName: content
```

The example configures four providers and two independent Milvus instances. `default-store` selects the store returned by `import vector` without an explicit key. Store map keys and the optional `key` field must remain unique.

## Legacy Option Mapping

| Legacy provider option | Unified configuration |
| --- | --- |
| Qdrant `payloadTextKey` | `properties.payloadTextKey` |
| Qdrant `host`, `port`, `apiKey` | `properties.host`, `properties.port`, `properties.apiKey` |
| Qdrant `utls` | `properties.useTls` (兼容别名仍支持；新字段优先) |
| Milvus `collectionName`, `dimension` | `properties.collectionName`, store `dimension` |
| Milvus `token` | `properties.apiKey` (兼容别名仍支持；新字段优先) |
| Milvus `indexType`, `metricType`, `consistencyLevel` | `properties.indexType`, `properties.metricType`, `properties.consistencyLevel` |
| Milvus `retrieveEmbeddingsOnSearch`, `autoFlushOnInsert` | `properties.retrieveEmbeddingsOnSearch`, `properties.autoFlushOnInsert` |
| Milvus `dbName` | `properties.databaseName` |
| Weaviate `avoidDups`, `consistencyLevel`, `textFieldName` | `properties.avoidDups`, `properties.consistencyLevel`, `properties.textFieldName` |
| Weaviate `useGrpcForInserts`, `securedGrpc`, `grpcPort` | `properties.useGrpcForInserts`, `properties.securedGrpc`, `properties.grpcPort` |

LangChain4j 0.35 does not expose a separate Weaviate gRPC host property. A legacy configuration that uses a gRPC host different from `host` is not directly supported by the unified starter; use the same host, provider-native configuration, or a custom `EmbeddingStoreProviderAdapter`.

## Script Migration

Use the unified module and an embedding-store key:

```javascript
import vector;

var store = vector['knowledge-milvus'].getOperations().getEmbeddingStore();
```

| Deprecated import | Replacement |
| --- | --- |
| `import milvus` | `import vector; vector['key']` |
| `import qdrant` | `import vector; vector['key']` |
| `import redisstack` | `import vector; vector['key']` |
| `import weaviate` | `import vector; vector['key']` |

The legacy modules resolve the requested key through `EmbeddingStoreRegistry`; they no longer construct provider-specific LangChain4j stores. Their datasource-management controllers and resource storage remain available during the transition, but do not configure embedding stores.
