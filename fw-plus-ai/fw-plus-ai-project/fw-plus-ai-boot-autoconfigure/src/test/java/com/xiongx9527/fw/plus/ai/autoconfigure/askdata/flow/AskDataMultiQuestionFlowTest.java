package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.memory.AskDataMemoryRecallAdapter;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.memory.AskDataRecommendationAdapter;
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
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataLongTermMemory;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataMemoryEntry;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataRecommendationService;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataMultiQuestionFlowTest {

    private static final String FIRST_QUESTION = "订单数量";
    private static final String SECOND_QUESTION = "退款数量";

    @Test
    public void shouldCreateAuditedChildResultsForExplicitTwoQuestionRequest() {
        AskDataResult result = executor(false).execute(request(FIRST_QUESTION, SECOND_QUESTION));

        List<AskDataResult> children = result.getChildResults();
        assertEquals(2, children.size());
        assertTrue(children.get(0).isSuccess());
        assertTrue(children.get(1).isSuccess());
        assertTrue(hasChildAuditStep(children.get(0)));
        assertTrue(hasChildAuditStep(children.get(1)));
    }

    @Test
    public void shouldKeepSuccessfulChildWhenAnotherExplicitQuestionFails() {
        AskDataResult result = executor(true).execute(request(FIRST_QUESTION, SECOND_QUESTION));

        List<AskDataResult> children = result.getChildResults();
        assertFalse(result.isSuccess());
        assertEquals(2, children.size());
        assertTrue(children.get(0).isSuccess());
        assertFalse(children.get(1).isSuccess());
    }

    @Test
    public void shouldRecallOnlyEntriesForCurrentProjectOperator() {
        AskDataLongTermMemory memory = AskDataLongTermMemory.inMemory();
        AskDataProjectContext current = new AskDataProjectContext("project-a", "operator-a");
        memory.remember(new AskDataMemoryEntry(current, "orders", "v1", "current operator"));
        memory.remember(new AskDataMemoryEntry(new AskDataProjectContext("project-a", "operator-b"), "orders", "v1",
                "another operator"));

        List<AskDataMemoryEntry> recalled = new AskDataMemoryRecallAdapter(memory).recall(current, "orders", "v1");

        assertEquals(1, recalled.size());
        assertEquals("current operator", recalled.get(0).getContent());
    }

    @Test
    public void shouldKeepDefaultRecommendationsWithinProjectOperatorScope() {
        AskDataRecommendationService recommendations = AskDataRecommendationService.inMemory();
        AskDataProjectContext current = new AskDataProjectContext("project-a", "operator-a");
        AskDataProjectContext another = new AskDataProjectContext("project-a", "operator-b");

        AskDataRecommendationAdapter adapter = new AskDataRecommendationAdapter(recommendations);
        adapter.record(current, "current operator");
        adapter.record(another, "another operator");

        assertEquals(Collections.singletonList("current operator"), adapter.recommend(current));
    }

    private AskDataRequest request(String... questions) {
        AskDataRequest request = new AskDataRequest();
        request.setQuestion(questions[0]);
        request.setProjectContext(new AskDataProjectContext("project-a", "operator-a"));
        request.setDatasourceKey("orders");
        request.setDialect("mysql");
        request.setModelKey("test-model");
        request.setExecute(Boolean.FALSE);
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("questions", Arrays.asList(questions));
        request.setOptions(options);
        return request;
    }

    private DefaultAskDataFlowExecutor executor(final boolean failSecondQuestion) {
        AskDataModelRegistry modelRegistry = new AskDataModelRegistry() {
            @Override
            public AskDataModelResponse generate(AskDataRequest request, String prompt) {
                if (failSecondQuestion && SECOND_QUESTION.equals(request.getQuestion())) {
                    return new AskDataModelResponse("not-json", "not-json", "test-model");
                }
                return new AskDataModelResponse("{\"sql\":\"select count(*) from orders\",\"brief\":\"ok\"}",
                        "response", "test-model");
            }

            @Override
            public AskDataChatModel getChatModel(String modelKey) {
                return null;
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
                return "prompt";
            }
        };
        AskDataSqlValidator validator = new AskDataSqlValidator() {
            @Override
            public AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows) {
                return AskDataSqlValidationResult.accepted(sql + " limit " + maxRows);
            }
        };
        AskDataSqlExecutor sqlExecutor = new AskDataSqlExecutor() {
            @Override
            public AskDataSqlExecutionResult execute(AskDataRequest request, String sql) {
                return AskDataSqlExecutionResult.success(Collections.emptyList(), Collections.emptyList());
            }
        };
        return new DefaultAskDataFlowExecutor(new AskDataProperties(), modelRegistry, metadataProvider, vectorRetriever,
                promptProvider, new AskDataDialectRegistry(), validator, sqlExecutor);
    }

    private boolean hasChildAuditStep(AskDataResult child) {
        for (com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataStepLog step : child.getSteps()) {
            if ("child-audit".equals(step.getStepKey()) && step.isSuccess()) {
                return true;
            }
        }
        return false;
    }
}
