package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.store;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.store.LangChain4jEmbeddingStoreProviderAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BuiltInLangChain4jEmbeddingStoreProviderTest {

    @Test
    public void shouldDeclareAllBuiltInProvidersWithoutADeadGenericProvider() throws Exception {
        Map<String, LangChain4jEmbeddingStoreProviderSpec> specs = specs();
        Map<String, ExpectedProvider> expected = expectedProviders();

        assertEquals(expected.keySet(), specs.keySet());
        assertEquals(23, specs.size());
        assertFalse(specs.containsKey("langChain4jEmbeddingStoreProviderAdapter"));
        for (Map.Entry<String, ExpectedProvider> entry : expected.entrySet()) {
            LangChain4jEmbeddingStoreProviderSpec spec = specs.get(entry.getKey());
            assertEquals(entry.getValue().providers, spec.getProviders(), entry.getKey());
            assertEquals(entry.getValue().className, spec.getStoreClassName(), entry.getKey());
            assertTrue(spec.getRequiredClassNames().contains(spec.getStoreClassName()), entry.getKey());
        }
    }

    @Test
    public void shouldPreserveRequiredAndFallbackRulesForSpecialProviders() throws Exception {
        Map<String, LangChain4jEmbeddingStoreProviderSpec> specs = specs();

        assertRequired(specs.get("redisStackEmbeddingStoreProviderAdapter"),
                "host", "port", "indexName", "dimension");
        assertRequiredAny(specs.get("milvusEmbeddingStoreProviderAdapter"), "host", "uri");
        assertRequired(specs.get("milvusEmbeddingStoreProviderAdapter"), "collectionName", "dimension");
        assertInvocation(specs.get("milvusEmbeddingStoreProviderAdapter"),
                "token", "apiKey", "token");
        assertInvocation(specs.get("qdrantEmbeddingStoreProviderAdapter"),
                "useTls", "useTls", "utls");
        assertInvocation(specs.get("elasticsearchEmbeddingStoreProviderAdapter"),
                "serverUrl", "uri");
        assertInvocation(specs.get("elasticsearchEmbeddingStoreProviderAdapter"),
                "userName", "username");
        assertRequiredAny(specs.get("azureAiSearchEmbeddingStoreProviderAdapter"),
                "dimensions", "dimension");
        assertInvocation(specs.get("azureAiSearchEmbeddingStoreProviderAdapter"),
                "dimensions", "dimensions", "dimension");
        assertInvocation(specs.get("azureAiSearchEmbeddingStoreProviderAdapter"),
                "tokenCredential", "tokenCredentialBean");
        assertRequiredAny(specs.get("azureCosmosNoSqlEmbeddingStoreProviderAdapter"),
                "cosmosClient", "cosmosClientBean");
        assertInvocation(specs.get("azureCosmosNoSqlEmbeddingStoreProviderAdapter"),
                "cosmosClient", "cosmosClient", "cosmosClientBean");
        assertConditionalRequirement(specs.get("neo4jEmbeddingStoreProviderAdapter"),
                LangChain4jEmbeddingStoreProviderSpec.ConditionKind.UNDEFINED, "driver",
                "uri", "username", "password");
        assertRequired(specs.get("infinispanEmbeddingStoreProviderAdapter"),
                "cacheName", "dimension");
    }

    @Test
    public void shouldPreserveConstructorAndOverloadPlansForComplexProviders() throws Exception {
        Map<String, LangChain4jEmbeddingStoreProviderSpec> specs = specs();

        LangChain4jEmbeddingStoreProviderSpec tablestore =
                specs.get("tablestoreEmbeddingStoreProviderAdapter");
        assertEquals(4, tablestore.getCreationPlans().size());
        assertEquals(Arrays.asList(2, 3, 9), constructorArities(tablestore));
        assertEquals(1, countPlans(tablestore,
                LangChain4jEmbeddingStoreProviderSpec.CreationKind.RAW_CONSTRUCTOR_ARGS));
        assertConditionalRequirement(tablestore,
                LangChain4jEmbeddingStoreProviderSpec.ConditionKind.PRESENT, "tableName",
                "searchIndexName", "pkName", "textField", "embeddingField");

        LangChain4jEmbeddingStoreProviderSpec oracle = specs.get("oracleEmbeddingStoreProviderAdapter");
        assertRequired(oracle, "dataSourceBean", "embeddingTable");
        assertEquals(Arrays.asList(1, 2), invocationArities(oracle, "embeddingTable"));
        assertInvocation(oracle, "dataSource", "dataSourceBean");
        assertInvocationCondition(oracle, "vectorIndex",
                LangChain4jEmbeddingStoreProviderSpec.ConditionKind.PRESENT, "vectorIndex");

        LangChain4jEmbeddingStoreProviderSpec weaviate =
                specs.get("weaviateEmbeddingStoreProviderAdapter");
        assertEquals(1, weaviate.getCreationPlans().size());
        assertEquals(LangChain4jEmbeddingStoreProviderSpec.CreationKind.CONSTRUCTOR,
                weaviate.getCreationPlans().get(0).getKind());
        assertEquals(12, weaviate.getCreationPlans().get(0).getConstructorArguments().size());
    }

    private Map<String, LangChain4jEmbeddingStoreProviderSpec> specs() throws Exception {
        LangChain4jEmbeddingStoreAdapterAutoConfiguration configuration =
                new LangChain4jEmbeddingStoreAdapterAutoConfiguration();
        StaticApplicationContext context = new StaticApplicationContext();
        Map<String, LangChain4jEmbeddingStoreProviderSpec> specs =
                new LinkedHashMap<String, LangChain4jEmbeddingStoreProviderSpec>();
        for (Method method : LangChain4jEmbeddingStoreAdapterAutoConfiguration.class.getDeclaredMethods()) {
            if (!LangChain4jEmbeddingStoreProviderAdapter.class.equals(method.getReturnType())
                    || method.getAnnotation(org.springframework.context.annotation.Bean.class) == null) {
                continue;
            }
            assertEquals(1, method.getParameterTypes().length, method.getName());
            Object adapter = method.invoke(configuration, context);
            Object spec = ReflectionTestUtils.getField(adapter, "spec");
            assertNotNull(spec, method.getName());
            specs.put(method.getName(), (LangChain4jEmbeddingStoreProviderSpec) spec);
        }
        return specs;
    }

    private void assertRequired(LangChain4jEmbeddingStoreProviderSpec spec, String... properties) {
        Set<String> expected = new LinkedHashSet<String>(Arrays.asList(properties));
        for (LangChain4jEmbeddingStoreProviderSpec.Requirement requirement : spec.getRequirements()) {
            if (!requirement.isAny()
                    && requirement.getCondition().getKind()
                    == LangChain4jEmbeddingStoreProviderSpec.ConditionKind.ALWAYS
                    && new LinkedHashSet<String>(requirement.getProperties()).equals(expected)) {
                return;
            }
        }
        throw new AssertionError("Required rule not found: " + expected + " in " + spec.getProviders());
    }

    private void assertRequiredAny(LangChain4jEmbeddingStoreProviderSpec spec, String... properties) {
        Set<String> expected = new LinkedHashSet<String>(Arrays.asList(properties));
        for (LangChain4jEmbeddingStoreProviderSpec.Requirement requirement : spec.getRequirements()) {
            if (requirement.isAny()
                    && new LinkedHashSet<String>(requirement.getProperties()).equals(expected)) {
                return;
            }
        }
        throw new AssertionError("Required-any rule not found: " + expected + " in " + spec.getProviders());
    }

    private void assertConditionalRequirement(LangChain4jEmbeddingStoreProviderSpec spec,
                                              LangChain4jEmbeddingStoreProviderSpec.ConditionKind kind,
                                              String conditionProperty, String... properties) {
        Set<String> expected = new LinkedHashSet<String>(Arrays.asList(properties));
        for (LangChain4jEmbeddingStoreProviderSpec.Requirement requirement : spec.getRequirements()) {
            if (containsCondition(requirement.getCondition(), kind, conditionProperty)
                    && new LinkedHashSet<String>(requirement.getProperties()).equals(expected)) {
                return;
            }
        }
        throw new AssertionError("Conditional rule not found: " + conditionProperty + " -> " + expected);
    }

    private boolean containsCondition(LangChain4jEmbeddingStoreProviderSpec.Condition condition,
                                      LangChain4jEmbeddingStoreProviderSpec.ConditionKind kind,
                                      String property) {
        if (condition.getKind() == kind && property.equals(condition.getProperty())) {
            return true;
        }
        for (LangChain4jEmbeddingStoreProviderSpec.Condition child : condition.getChildren()) {
            if (containsCondition(child, kind, property)) {
                return true;
            }
        }
        return false;
    }

    private void assertInvocation(LangChain4jEmbeddingStoreProviderSpec spec,
                                  String method, String... sources) {
        List<String> expected = Arrays.asList(sources);
        for (LangChain4jEmbeddingStoreProviderSpec.CreationPlan plan : spec.getCreationPlans()) {
            for (LangChain4jEmbeddingStoreProviderSpec.Invocation invocation : plan.getInvocations()) {
                if (method.equals(invocation.getMethod()) && invocation.getArguments().size() == 1
                        && invocation.getArguments().get(0).getSources().equals(expected)) {
                    return;
                }
            }
        }
        throw new AssertionError("Invocation not found: " + method + " <- " + expected);
    }

    private List<Integer> constructorArities(LangChain4jEmbeddingStoreProviderSpec spec) {
        List<Integer> arities = new ArrayList<Integer>();
        for (LangChain4jEmbeddingStoreProviderSpec.CreationPlan plan : spec.getCreationPlans()) {
            if (plan.getKind() == LangChain4jEmbeddingStoreProviderSpec.CreationKind.CONSTRUCTOR) {
                arities.add(plan.getConstructorArguments().size());
            }
        }
        Collections.sort(arities);
        return arities;
    }

    private void assertInvocationCondition(LangChain4jEmbeddingStoreProviderSpec spec,
                                           String method,
                                           LangChain4jEmbeddingStoreProviderSpec.ConditionKind kind,
                                           String property) {
        for (LangChain4jEmbeddingStoreProviderSpec.CreationPlan plan : spec.getCreationPlans()) {
            for (LangChain4jEmbeddingStoreProviderSpec.Invocation invocation : plan.getInvocations()) {
                if (method.equals(invocation.getMethod())
                        && containsCondition(invocation.getCondition(), kind, property)) {
                    return;
                }
            }
        }
        throw new AssertionError("Invocation condition not found: " + method + " " + kind + " " + property);
    }

    private int countPlans(LangChain4jEmbeddingStoreProviderSpec spec,
                           LangChain4jEmbeddingStoreProviderSpec.CreationKind kind) {
        int count = 0;
        for (LangChain4jEmbeddingStoreProviderSpec.CreationPlan plan : spec.getCreationPlans()) {
            if (plan.getKind() == kind) {
                count++;
            }
        }
        return count;
    }

    private List<Integer> invocationArities(LangChain4jEmbeddingStoreProviderSpec spec, String method) {
        List<Integer> arities = new ArrayList<Integer>();
        for (LangChain4jEmbeddingStoreProviderSpec.CreationPlan plan : spec.getCreationPlans()) {
            for (LangChain4jEmbeddingStoreProviderSpec.Invocation invocation : plan.getInvocations()) {
                if (method.equals(invocation.getMethod())) {
                    arities.add(invocation.getArguments().size());
                }
            }
        }
        Collections.sort(arities);
        return arities;
    }

    private Map<String, ExpectedProvider> expectedProviders() {
        String prefix = "dev.langchain4j.store.embedding.";
        Map<String, ExpectedProvider> expected = new LinkedHashMap<String, ExpectedProvider>();
        expected.put("redisStackEmbeddingStoreProviderAdapter", provider(prefix + "redis.RedisEmbeddingStore", "redisstack", "redis"));
        expected.put("milvusEmbeddingStoreProviderAdapter", provider(prefix + "milvus.MilvusEmbeddingStore", "milvus"));
        expected.put("elasticsearchEmbeddingStoreProviderAdapter", provider(prefix + "elasticsearch.ElasticsearchEmbeddingStore", "elasticsearch"));
        expected.put("qdrantEmbeddingStoreProviderAdapter", provider(prefix + "qdrant.QdrantEmbeddingStore", "qdrant"));
        expected.put("weaviateEmbeddingStoreProviderAdapter", provider(prefix + "weaviate.WeaviateEmbeddingStore", "weaviate"));
        expected.put("astraDbEmbeddingStoreProviderAdapter", provider(prefix + "astradb.AstraDbEmbeddingStore", "astra-db"));
        expected.put("azureAiSearchEmbeddingStoreProviderAdapter", provider(prefix + "azure.search.AzureAiSearchEmbeddingStore", "azure-ai-search"));
        expected.put("azureCosmosMongoVCoreEmbeddingStoreProviderAdapter", provider(prefix + "azure.cosmos.mongo.vcore.AzureCosmosDbMongoVCoreEmbeddingStore", "azure-cosmos-mongo-vcore"));
        expected.put("azureCosmosNoSqlEmbeddingStoreProviderAdapter", provider(prefix + "azure.cosmos.nosql.AzureCosmosDbNoSqlEmbeddingStore", "azure-cosmos-nosql"));
        expected.put("cassandraEmbeddingStoreProviderAdapter", provider(prefix + "cassandra.CassandraEmbeddingStore", "cassandra"));
        expected.put("chromaEmbeddingStoreProviderAdapter", provider(prefix + "chroma.ChromaEmbeddingStore", "chroma"));
        expected.put("couchbaseEmbeddingStoreProviderAdapter", provider(prefix + "couchbase.CouchbaseEmbeddingStore", "couchbase"));
        expected.put("inMemoryEmbeddingStoreProviderAdapter", provider(prefix + "inmemory.InMemoryEmbeddingStore", "in-memory", "inmemory", "memory"));
        expected.put("infinispanEmbeddingStoreProviderAdapter", provider(prefix + "infinispan.InfinispanEmbeddingStore", "infinispan"));
        expected.put("mongoDbEmbeddingStoreProviderAdapter", provider(prefix + "mongodb.MongoDbEmbeddingStore", "mongodb-atlas", "mongodb"));
        expected.put("neo4jEmbeddingStoreProviderAdapter", provider(prefix + "neo4j.Neo4jEmbeddingStore", "neo4j"));
        expected.put("openSearchEmbeddingStoreProviderAdapter", provider(prefix + "opensearch.OpenSearchEmbeddingStore", "opensearch"));
        expected.put("oracleEmbeddingStoreProviderAdapter", provider(prefix + "oracle.OracleEmbeddingStore", "oracle"));
        expected.put("pgVectorEmbeddingStoreProviderAdapter", provider(prefix + "pgvector.PgVectorEmbeddingStore", "pgvector", "postgres", "postgresql"));
        expected.put("pineconeEmbeddingStoreProviderAdapter", provider(prefix + "pinecone.PineconeEmbeddingStore", "pinecone"));
        expected.put("tablestoreEmbeddingStoreProviderAdapter", provider(prefix + "tablestore.TablestoreEmbeddingStore", "tablestore"));
        expected.put("vearchEmbeddingStoreProviderAdapter", provider(prefix + "vearch.VearchEmbeddingStore", "vearch"));
        expected.put("vespaEmbeddingStoreProviderAdapter", provider(prefix + "vespa.VespaEmbeddingStore", "vespa"));
        return expected;
    }

    private ExpectedProvider provider(String className, String... providers) {
        return new ExpectedProvider(className,
                new LinkedHashSet<String>(Arrays.asList(providers)));
    }

    private static final class ExpectedProvider {
        private final String className;
        private final Set<String> providers;
        private ExpectedProvider(String className, Set<String> providers) {
            this.className = className;
            this.providers = providers;
        }
    }
}
