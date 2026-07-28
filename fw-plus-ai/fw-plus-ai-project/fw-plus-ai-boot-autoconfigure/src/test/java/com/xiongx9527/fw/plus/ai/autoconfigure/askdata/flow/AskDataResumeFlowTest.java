package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataCheckpoint;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.DefaultAskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.InMemoryAskDataConversationStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataChatModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataEmbeddingModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialect;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialectRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataPromptTemplateProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlExecutionResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlExecutor;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlValidationResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql.AskDataSqlValidator;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataRetrievedDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorQuery;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetriever;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataResumeFlowTest {

    @Test
    public void shouldResumeValidatedSqlWithoutRepeatingEarlierSteps() {
        Fixture fixture = new Fixture();
        fixture.store.save(fixture.validatedCheckpoint("resume-validate", "1"));

        AskDataRequest request = fixture.request();
        request.setResumeToken("resume-validate");
        AskDataResult result = fixture.executor().execute(request);

        assertTrue(result.isSuccess());
        assertEquals("select * from orders limit 100", result.getSql());
        assertEquals(0, fixture.metadataCalls);
        assertEquals(0, fixture.promptCalls);
        assertEquals(0, fixture.modelCalls);
        assertEquals(0, fixture.validatorCalls);
        assertEquals(1, fixture.executorCalls);
    }

    @Test
    public void shouldReturnCheckpointNotFoundWhenResumeTokenDoesNotExist() {
        Fixture fixture = new Fixture();
        AskDataRequest request = fixture.request();
        request.setResumeToken("missing");

        AskDataResult result = fixture.executor().execute(request);

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.CHECKPOINT_NOT_FOUND, result.getErrorCode());
    }

    @Test
    public void shouldReturnResumeFailedWhenCheckpointVersionDoesNotMatch() {
        Fixture fixture = new Fixture();
        fixture.store.save(fixture.validatedCheckpoint("resume-version", "outdated"));
        AskDataRequest request = fixture.request();
        request.setResumeToken("resume-version");

        AskDataResult result = fixture.executor().execute(request);

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.RESUME_FAILED, result.getErrorCode());
        assertEquals(0, fixture.executorCalls);
    }

    @Test
    public void shouldReturnResumeFailedWithoutExecutingUnvalidatedSql() {
        Fixture fixture = new Fixture();
        AskDataCheckpoint checkpoint = fixture.validatedCheckpoint("resume-unvalidated", "1");
        checkpoint.setSqlValidated(Boolean.FALSE);
        fixture.store.save(checkpoint);
        AskDataRequest request = fixture.request();
        request.setResumeToken("resume-unvalidated");

        AskDataResult result = fixture.executor().execute(request);

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.RESUME_FAILED, result.getErrorCode());
        assertEquals(0, fixture.executorCalls);
    }

    @Test
    public void shouldRejectResumeWhenRequestIdentityDoesNotMatch() {
        Fixture fixture = new Fixture();
        fixture.store.save(fixture.validatedCheckpoint("resume-identity", "1"));
        AskDataRequest request = fixture.request();
        request.setResumeToken("resume-identity");
        request.setDatasourceKey("other-datasource");

        AskDataResult result = fixture.executor().execute(request);

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.RESUME_FAILED, result.getErrorCode());
        assertEquals(0, fixture.executorCalls);
    }

    @Test
    public void shouldRejectResumeWhenQuestionModelRowsOrExecuteDoesNotMatch() {
        assertIdentityMismatch("question");
        assertIdentityMismatch("model");
        assertIdentityMismatch("maxRows");
        assertIdentityMismatch("execute");
    }

    @Test
    public void shouldRejectResumeWhenEffectiveExecutionPolicyChanged() {
        Fixture fixture = new Fixture();
        fixture.store.save(fixture.validatedCheckpoint("resume-policy", "1"));
        fixture.properties.getSql().setExecuteEnabled(false);
        AskDataRequest request = fixture.request();
        request.setResumeToken("resume-policy");

        AskDataResult result = fixture.executor().execute(request);

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.RESUME_FAILED, result.getErrorCode());
        assertEquals(0, fixture.executorCalls);
    }

    @Test
    public void shouldRejectResumeWhenTenantOptionsDrift() {
        Fixture fixture = new Fixture();
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("tenantId", "tenant-a");
        AskDataRequest initial = fixture.requestWithoutConversation();
        initial.setOptions(options);

        AskDataResult initialResult = fixture.executor().execute(initial);
        options.put("tenantId", "tenant-b");
        AskDataRequest resume = fixture.request();
        resume.setConversationId(initialResult.getConversationId());
        resume.setOptions(options);

        AskDataResult resumed = fixture.executor().execute(resume);

        assertTrue(initialResult.isSuccess());
        assertFalse(resumed.isSuccess());
        assertEquals(AskDataErrorCode.RESUME_FAILED, resumed.getErrorCode());
        assertEquals(1, fixture.executorCalls);
    }

    @Test
    public void shouldRejectResumeWhenOptionsCannotBeSafelyCompared() {
        Fixture fixture = new Fixture();
        Map<String, Object> options = new HashMap<String, Object>();
        options.put("tenantContext", new Object());
        AskDataRequest initial = fixture.requestWithoutConversation();
        initial.setOptions(options);

        AskDataResult initialResult = fixture.executor().execute(initial);
        AskDataRequest resume = fixture.request();
        resume.setConversationId(initialResult.getConversationId());
        resume.setOptions(options);

        AskDataResult resumed = fixture.executor().execute(resume);

        assertTrue(initialResult.isSuccess());
        assertFalse(resumed.isSuccess());
        assertEquals(AskDataErrorCode.RESUME_FAILED, resumed.getErrorCode());
        assertEquals(1, fixture.executorCalls);
    }

    @Test
    public void shouldRejectResumeAfterExecutionWasStartedButCompletionWasNotCheckpointed() {
        Fixture fixture = new Fixture();
        fixture.failExecution = true;

        AskDataResult first = fixture.executor().execute(fixture.requestWithoutConversation());
        AskDataRequest resume = fixture.request();
        resume.setConversationId(first.getConversationId());
        resume.setResumeToken(null);
        AskDataResult resumed = fixture.executor().execute(resume);

        assertFalse(first.isSuccess());
        assertFalse(resumed.isSuccess());
        assertEquals(AskDataErrorCode.RESUME_FAILED, resumed.getErrorCode());
        assertEquals(1, fixture.executorCalls);
    }

    @Test
    public void shouldNotExecuteSqlAgainWhenResumingExecutedCheckpoint() {
        Fixture fixture = new Fixture();
        AskDataCheckpoint checkpoint = fixture.validatedCheckpoint("resume-executed", "1");
        checkpoint.setCompletedSteps(Arrays.asList("metadata", "prompt", "generate-sql", "validate-sql",
                "execute-sql"));
        checkpoint.setCurrentStep("execute-sql");
        checkpoint.setSqlExecuted(Boolean.TRUE);
        checkpoint.setRowCount(Integer.valueOf(3));
        fixture.store.save(checkpoint);
        AskDataRequest request = fixture.request();
        request.setResumeToken("resume-executed");

        AskDataResult result = fixture.executor().execute(request);

        assertTrue(result.isSuccess());
        assertEquals(Integer.valueOf(3), result.getRowCount());
        assertEquals(0, fixture.executorCalls);
    }

    @Test
    public void shouldRejectUnexecutedCheckpointWithRowCountWithoutExecutingSql() {
        Fixture fixture = new Fixture();
        AskDataCheckpoint checkpoint = fixture.validatedCheckpoint("resume-unexecuted-row-count", "1");
        checkpoint.setCompletedSteps(Arrays.asList("metadata", "prompt", "generate-sql", "validate-sql",
                "execute-sql"));
        checkpoint.setCurrentStep("execute-sql");
        checkpoint.setSqlExecuted(Boolean.FALSE);
        checkpoint.setRowCount(Integer.valueOf(3));
        fixture.store.save(checkpoint);
        AskDataRequest request = fixture.request();
        request.setResumeToken("resume-unexecuted-row-count");

        AskDataResult result = fixture.executor().execute(request);

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.RESUME_FAILED, result.getErrorCode());
        assertEquals(0, fixture.executorCalls);
    }

    @Test
    public void shouldRejectNonContinuousCompletedStepsWithoutExecutingSql() {
        Fixture fixture = new Fixture();
        AskDataCheckpoint checkpoint = fixture.validatedCheckpoint("resume-gap", "1");
        checkpoint.setCompletedSteps(Arrays.asList("metadata", "validate-sql"));
        checkpoint.setCurrentStep("validate-sql");
        fixture.store.save(checkpoint);
        AskDataRequest request = fixture.request();
        request.setResumeToken("resume-gap");

        AskDataResult result = fixture.executor().execute(request);

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.RESUME_FAILED, result.getErrorCode());
        assertEquals(0, fixture.executorCalls);
    }

    @Test
    public void shouldResumeMetadataCheckpointByReloadingStructuredMetadata() {
        Fixture fixture = new Fixture();
        AskDataCheckpoint checkpoint = new AskDataCheckpoint();
        checkpoint.setConversationId("conversation-1");
        checkpoint.setCheckpointId("checkpoint-metadata");
        checkpoint.setResumeToken("resume-metadata");
        checkpoint.setVersion("1");
        checkpoint.setDatasourceKey("orders");
        checkpoint.setDialect("mysql");
        checkpoint.setModelKey("model-1");
        checkpoint.setRequestSummary("订单数量");
        checkpoint.setMaxRows(Integer.valueOf(100));
        checkpoint.setExecute(Boolean.TRUE);
        checkpoint.setRequestOptionsSnapshot("null");
        checkpoint.setCompletedSteps(Collections.singletonList("metadata"));
        checkpoint.setCurrentStep("metadata");
        fixture.store.save(checkpoint);
        AskDataRequest request = fixture.request();
        request.setResumeToken("resume-metadata");

        AskDataResult result = fixture.executor().execute(request);

        assertTrue(result.isSuccess());
        assertEquals(1, fixture.metadataCalls);
        assertEquals(1, fixture.promptCalls);
        assertEquals(1, fixture.modelCalls);
        assertEquals(1, fixture.validatorCalls);
        assertEquals(1, fixture.executorCalls);
    }

    @Test
    public void shouldCreateConversationCheckpointForNewRequest() {
        Fixture fixture = new Fixture();

        AskDataResult result = fixture.executor().execute(fixture.requestWithoutConversation());

        assertTrue(result.isSuccess());
        assertTrue(result.getConversationId() != null && result.getConversationId().length() > 0);
        assertTrue(fixture.store.loadLatest(result.getConversationId()) != null);
    }

    @Test
    public void shouldReturnCompressionFailureWhenCheckpointCompressionFails() {
        Fixture fixture = new Fixture();
        AskDataContextCompressor failingCompressor = new AskDataContextCompressor() {
            @Override
            public com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataCompressedContext compress(
                    AskDataStepContext context) {
                throw new IllegalStateException("compression unavailable");
            }
        };

        AskDataResult result = fixture.executor(failingCompressor).execute(fixture.requestWithoutConversation());

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.CONTEXT_COMPRESSION_FAILED, result.getErrorCode());
    }

    private static class Fixture {

        private final AskDataProperties properties = new AskDataProperties();
        private final AskDataConversationStore store = new InMemoryAskDataConversationStore();
        private final AskDataContextCompressor compressor = new DefaultAskDataContextCompressor(properties);
        private int metadataCalls;
        private int promptCalls;
        private int modelCalls;
        private int validatorCalls;
        private int executorCalls;
        private boolean failExecution;

        private Fixture() {
            properties.getSql().setExecuteEnabled(true);
            properties.getConversation().setCheckpointVersion("1");
        }

        private AskDataRequest request() {
            AskDataRequest request = new AskDataRequest();
            request.setConversationId("conversation-1");
            request.setDatasourceKey("orders");
            request.setDialect("mysql");
            request.setModelKey("model-1");
            request.setQuestion("订单数量");
            request.setProjectContext(new AskDataProjectContext("project-a", "operator-a"));
            request.setExecute(Boolean.TRUE);
            request.setMaxRows(Integer.valueOf(100));
            return request;
        }

        private AskDataRequest requestWithoutConversation() {
            AskDataRequest request = request();
            request.setConversationId(null);
            return request;
        }

        private AskDataCheckpoint validatedCheckpoint(String token, String version) {
            AskDataCheckpoint checkpoint = new AskDataCheckpoint();
            checkpoint.setConversationId("conversation-1");
            checkpoint.setCheckpointId("checkpoint-1");
            checkpoint.setResumeToken(token);
            checkpoint.setVersion(version);
            checkpoint.setDatasourceKey("orders");
            checkpoint.setDialect("mysql");
            checkpoint.setModelKey("model-1");
            checkpoint.setRequestSummary("订单数量");
            checkpoint.setMaxRows(Integer.valueOf(100));
            checkpoint.setExecute(Boolean.TRUE);
            checkpoint.setRequestOptionsSnapshot("null");
            checkpoint.setSql("select * from orders limit 100");
            checkpoint.setSqlValidated(Boolean.TRUE);
            checkpoint.setCompletedSteps(Arrays.asList("metadata", "prompt", "generate-sql", "validate-sql"));
            checkpoint.setCurrentStep("validate-sql");
            return checkpoint;
        }

        private DefaultAskDataFlowExecutor executor() {
            return executor(compressor);
        }

        private DefaultAskDataFlowExecutor executor(AskDataContextCompressor contextCompressor) {
            AskDataModelRegistry modelRegistry = new AskDataModelRegistry() {
                @Override
                public AskDataChatModel getChatModel(String modelKey) {
                    modelCalls++;
                    return new AskDataChatModel() {
                        @Override
                        public AskDataModelResponse generate(String prompt) {
                            return new AskDataModelResponse("{\"sql\":\"select 1\"}", null, "model-1");
                        }
                    };
                }

                @Override
                public AskDataEmbeddingModel getEmbeddingModel(String modelKey) {
                    return null;
                }

                @Override
                public String getDefaultChatModelKey() {
                    return "model-1";
                }

                @Override
                public String getDefaultEmbeddingModelKey() {
                    return null;
                }
            };
            AskDataMetadataProvider metadataProvider = new AskDataMetadataProvider() {
                @Override
                public AskDataMetadata load(AskDataRequest request) {
                    metadataCalls++;
                    return new AskDataMetadata("mysql", Collections.emptyList(), Collections.emptyList(),
                            Collections.emptyList(), Collections.emptyList());
                }
            };
            AskDataVectorRetriever vectorRetriever = new AskDataVectorRetriever() {
                @Override
                public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
                    return Collections.emptyList();
                }
            };
            AskDataPromptTemplateProvider promptProvider = new AskDataPromptTemplateProvider() {
                @Override
                public String buildPrompt(AskDataRequest request, AskDataMetadata metadata,
                        List<AskDataRetrievedDocument> documents, int maxRows) {
                    promptCalls++;
                    return "prompt";
                }
            };
            AskDataSqlValidator validator = new AskDataSqlValidator() {
                @Override
                public AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows) {
                    validatorCalls++;
                    return AskDataSqlValidationResult.accepted(sql);
                }
            };
            AskDataSqlExecutor sqlExecutor = new AskDataSqlExecutor() {
                @Override
                public AskDataSqlExecutionResult execute(AskDataRequest request, String sql) {
                    executorCalls++;
                    if (failExecution) {
                        throw new IllegalStateException("connection lost after execute");
                    }
                    return AskDataSqlExecutionResult.success(Collections.emptyList(), Collections.emptyList());
                }
            };
            return new DefaultAskDataFlowExecutor(properties, modelRegistry, metadataProvider, vectorRetriever,
                    promptProvider, new AskDataDialectRegistry(), validator, sqlExecutor, store, contextCompressor);
        }
    }

    private void assertIdentityMismatch(String field) {
        Fixture fixture = new Fixture();
        fixture.store.save(fixture.validatedCheckpoint("resume-" + field, "1"));
        AskDataRequest request = fixture.request();
        request.setResumeToken("resume-" + field);
        if ("question".equals(field)) {
            request.setQuestion("客户数量");
        } else if ("model".equals(field)) {
            request.setModelKey("model-2");
        } else if ("maxRows".equals(field)) {
            request.setMaxRows(Integer.valueOf(50));
        } else {
            request.setExecute(Boolean.FALSE);
        }

        AskDataResult result = fixture.executor().execute(request);

        assertFalse(result.isSuccess());
        assertEquals(AskDataErrorCode.RESUME_FAILED, result.getErrorCode());
        assertEquals(0, fixture.executorCalls);
    }
}
