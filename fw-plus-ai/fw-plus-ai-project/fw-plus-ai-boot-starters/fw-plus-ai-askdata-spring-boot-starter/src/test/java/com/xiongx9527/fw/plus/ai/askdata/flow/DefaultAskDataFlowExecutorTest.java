package com.xiongx9527.fw.plus.ai.askdata.flow;

import com.xiongx9527.fw.plus.ai.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.askdata.AskDataStepLog;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataEmbeddingModel;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelResponse;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataDialect;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataDialectRegistry;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlExecutionResult;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidationResult;
import com.xiongx9527.fw.plus.ai.askdata.sql.AskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataRetrievedDocument;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorQuery;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataVectorRetriever;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DefaultAskDataFlowExecutorTest {

    private static final String SQL = "select count(*) from orders limit 100";

    @Test
    public void shouldExecuteDefaultFlowInOrder() {
        StubDependencies dependencies = new StubDependencies();
        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\","
                + "\"tables\":[\"orders\"],\"chartType\":\"number\",\"brief\":\"订单数\"}")
                .execute(request(true));

        assertEquals(Arrays.asList("metadata", "prompt", "generate-sql", "validate-sql", "execute-sql", "format-result"),
                stepKeys(result));
        assertEquals(SQL, result.getSql());
        assertTrue(result.isSuccess());
        assertEquals(1, dependencies.executorCalls);
        assertEquals(Arrays.asList("schema", "term", "example"), dependencies.retrieveTypes);
    }

    @Test
    public void shouldReturnModelNotFoundWhenConfiguredModelIsMissing() {
        StubDependencies dependencies = new StubDependencies();
        AskDataResult result = dependencies.executor(null).execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.MODEL_NOT_FOUND, result.getErrorCode());
    }

    @Test
    public void shouldReturnInvalidResponseWhenModelDoesNotProduceJson() {
        StubDependencies dependencies = new StubDependencies();
        AskDataResult result = dependencies.executor("not json").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.MODEL_RESPONSE_INVALID, result.getErrorCode());
    }

    @Test
    public void shouldShortCircuitBeforeExecutorWhenSqlIsRejected() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.validationResult = AskDataSqlValidationResult.rejected(AskDataErrorCode.SQL_REJECTED, "only select allowed");

        AskDataResult result = dependencies.executor("{\"sql\":\"delete from orders\"}").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.SQL_REJECTED, result.getErrorCode());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldReturnSqlWithoutRowsWhenExecutionIsDisabledByRequest() {
        StubDependencies dependencies = new StubDependencies();
        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}").execute(request(false));

        assertTrue(result.isSuccess());
        assertEquals(SQL, result.getSql());
        assertTrue(result.getRows().isEmpty());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldContainVectorRetrieverFailureInResultWithoutExecutingSql() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.vectorRetriever = new AskDataVectorRetriever() {
            @Override
            public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
                throw new IllegalStateException("vector unavailable");
            }
        };

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals("VECTOR_RETRIEVAL_FAILED", result.getErrorCode());
        assertEquals(Arrays.asList("metadata", "prompt"), stepKeys(result));
        assertFalse(result.getSteps().get(1).isSuccess());
        assertEquals(0, dependencies.executorCalls);
    }

    @Test
    public void shouldContainDialectRegistryFailureInResultWithoutExecutingSql() {
        StubDependencies dependencies = new StubDependencies();
        dependencies.dialectRegistry = new AskDataDialectRegistry() {
            @Override
            public AskDataDialect getDialect(String dialect) {
                throw new IllegalStateException("dialect unavailable");
            }
        };

        AskDataResult result = dependencies.executor("{\"sql\":\"select count(*) from orders\"}").execute(request(true));

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.DIALECT_RESOLUTION_FAILED, result.getErrorCode());
        assertEquals(Arrays.asList("metadata", "prompt", "generate-sql", "validate-sql"), stepKeys(result));
        assertFalse(result.getSteps().get(3).isSuccess());
        assertEquals(0, dependencies.executorCalls);
    }

    private AskDataRequest request(boolean execute) {
        AskDataRequest request = new AskDataRequest();
        request.setQuestion("订单数量");
        request.setDatasourceKey("orders");
        request.setDialect("mysql");
        request.setModelKey("test-model");
        request.setExecute(Boolean.valueOf(execute));
        return request;
    }

    private List<String> stepKeys(AskDataResult result) {
        java.util.ArrayList<String> keys = new java.util.ArrayList<String>();
        for (AskDataStepLog step : result.getSteps()) {
            keys.add(step.getStepKey());
        }
        return keys;
    }

    private static class StubDependencies {

        private final AskDataProperties properties = new AskDataProperties();
        private AskDataDialectRegistry dialectRegistry = new AskDataDialectRegistry();
        private AskDataVectorRetriever vectorRetriever;
        private final List<String> retrieveTypes = new java.util.ArrayList<String>();
        private int executorCalls;
        private AskDataSqlValidationResult validationResult = AskDataSqlValidationResult.accepted(SQL);

        private DefaultAskDataFlowExecutor executor(final String modelResponse) {
            properties.getSql().setExecuteEnabled(true);
            AskDataModelRegistry modelRegistry = new AskDataModelRegistry() {
                @Override
                public AskDataChatModel getChatModel(String modelKey) {
                    if (modelResponse == null) {
                        return null;
                    }
                    return new AskDataChatModel() {
                        @Override
                        public AskDataModelResponse generate(String prompt) {
                            return new AskDataModelResponse(modelResponse, modelResponse, "test-model");
                        }
                    };
                }

                @Override
                public AskDataEmbeddingModel getEmbeddingModel(String modelKey) {
                    return null;
                }

                @Override
                public String getDefaultChatModelKey() {
                    return "test-model";
                }

                @Override
                public String getDefaultEmbeddingModelKey() {
                    return null;
                }
            };
            AskDataMetadataProvider metadataProvider = new AskDataMetadataProvider() {
                @Override
                public AskDataMetadata load(AskDataRequest request) {
                    return new AskDataMetadata("mysql", Collections.emptyList(), Collections.emptyList(),
                            Collections.emptyList(), Collections.emptyList());
                }
            };
            if (vectorRetriever == null) {
                vectorRetriever = new AskDataVectorRetriever() {
                    @Override
                    public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
                        retrieveTypes.add(query.getRetrieveType());
                        return Collections.emptyList();
                    }
                };
            }
            AskDataPromptTemplateProvider promptProvider = new AskDataPromptTemplateProvider() {
                @Override
                public String buildPrompt(AskDataRequest request, AskDataMetadata metadata,
                        List<AskDataRetrievedDocument> documents, int maxRows) {
                    return "prompt";
                }
            };
            AskDataSqlValidator sqlValidator = new AskDataSqlValidator() {
                @Override
                public AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows) {
                    return validationResult;
                }
            };
            AskDataSqlExecutor sqlExecutor = new AskDataSqlExecutor() {
                @Override
                public AskDataSqlExecutionResult execute(AskDataRequest request, String sql) {
                    executorCalls++;
                    return AskDataSqlExecutionResult.success(Collections.emptyList(), Collections.emptyList());
                }
            };
            return new DefaultAskDataFlowExecutor(properties, modelRegistry, metadataProvider, vectorRetriever,
                    promptProvider, dialectRegistry, sqlValidator, sqlExecutor);
        }
    }
}
