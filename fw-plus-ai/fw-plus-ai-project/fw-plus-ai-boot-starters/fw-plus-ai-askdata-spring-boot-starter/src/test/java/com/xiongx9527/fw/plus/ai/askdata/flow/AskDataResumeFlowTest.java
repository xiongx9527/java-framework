package com.xiongx9527.fw.plus.ai.askdata.flow;

import com.xiongx9527.fw.plus.ai.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.askdata.conversation.AskDataCheckpoint;
import com.xiongx9527.fw.plus.ai.askdata.conversation.AskDataConversationStore;
import com.xiongx9527.fw.plus.ai.askdata.conversation.AskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.askdata.conversation.DefaultAskDataContextCompressor;
import com.xiongx9527.fw.plus.ai.askdata.conversation.InMemoryAskDataConversationStore;
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
            public com.xiongx9527.fw.plus.ai.askdata.conversation.AskDataCompressedContext compress(
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
            request.setExecute(Boolean.TRUE);
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
            checkpoint.setSql("select * from orders limit 100");
            checkpoint.setSqlValidated(Boolean.TRUE);
            checkpoint.setCompletedSteps(Arrays.asList("metadata", "generate-sql", "validate-sql"));
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
                    return AskDataSqlExecutionResult.success(Collections.emptyList(), Collections.emptyList());
                }
            };
            return new DefaultAskDataFlowExecutor(properties, modelRegistry, metadataProvider, vectorRetriever,
                    promptProvider, new AskDataDialectRegistry(), validator, sqlExecutor, store, contextCompressor);
        }
    }
}
