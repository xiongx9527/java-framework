package com.xiongx9527.fw.plus.ai.askdata.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadataProvider;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelResponse;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRegistry;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultAskDataFlowExecutor implements AskDataFlowExecutor {

    private final AskDataProperties properties;
    private final AskDataModelRegistry modelRegistry;
    private final AskDataMetadataProvider metadataProvider;
    private final AskDataVectorRetriever vectorRetriever;
    private final AskDataPromptTemplateProvider promptTemplateProvider;
    private final AskDataDialectRegistry dialectRegistry;
    private final AskDataSqlValidator sqlValidator;
    private final AskDataSqlExecutor sqlExecutor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DefaultAskDataFlowExecutor(AskDataProperties properties, AskDataModelRegistry modelRegistry,
            AskDataMetadataProvider metadataProvider, AskDataVectorRetriever vectorRetriever,
            AskDataPromptTemplateProvider promptTemplateProvider, AskDataDialectRegistry dialectRegistry,
            AskDataSqlValidator sqlValidator, AskDataSqlExecutor sqlExecutor) {
        this.properties = properties;
        this.modelRegistry = modelRegistry;
        this.metadataProvider = metadataProvider;
        this.vectorRetriever = vectorRetriever;
        this.promptTemplateProvider = promptTemplateProvider;
        this.dialectRegistry = dialectRegistry;
        this.sqlValidator = sqlValidator;
        this.sqlExecutor = sqlExecutor;
    }

    @Override
    public AskDataResult execute(AskDataRequest request) {
        AskDataResult result = new AskDataResult();
        AskDataStepContext context = new AskDataStepContext(request);
        AskDataMetadata metadata;
        try {
            metadata = metadataProvider.load(request);
        } catch (RuntimeException ex) {
            return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND, message(ex));
        }
        if (metadata == null) {
            return failure(result, "metadata", AskDataErrorCode.METADATA_NOT_FOUND, "AskData metadata was not found");
        }
        context.setMetadata(metadata);
        result.addStep("metadata", "Metadata", true, "Metadata loaded");

        int maxRows = maxRows(request);
        List<AskDataRetrievedDocument> documents = retrieveDocuments(request);
        context.setRetrievedDocuments(documents);
        String prompt;
        try {
            prompt = promptTemplateProvider.buildPrompt(request, metadata, documents, maxRows);
        } catch (RuntimeException ex) {
            return failure(result, "prompt", AskDataErrorCode.MODEL_RESPONSE_INVALID, message(ex));
        }
        context.setPrompt(prompt);
        result.addStep("prompt", "Prompt", true, "Prompt built");

        AskDataChatModel model;
        try {
            model = modelRegistry.getChatModel(request == null ? null : request.getModelKey());
        } catch (RuntimeException ex) {
            return failure(result, "generate-sql", AskDataErrorCode.MODEL_NOT_FOUND, message(ex));
        }
        if (model == null) {
            return failure(result, "generate-sql", AskDataErrorCode.MODEL_NOT_FOUND, "AskData chat model was not found");
        }
        AskDataModelResponse modelResponse;
        try {
            modelResponse = model.generate(prompt);
        } catch (RuntimeException ex) {
            return failure(result, "generate-sql", AskDataErrorCode.MODEL_RESPONSE_INVALID, message(ex));
        }
        if (modelResponse == null || isBlank(modelResponse.getText())) {
            return failure(result, "generate-sql", AskDataErrorCode.MODEL_RESPONSE_INVALID,
                    "AskData model response is empty");
        }
        result.setRawModelResponse(modelResponse.getText());
        JsonNode modelJson;
        try {
            modelJson = objectMapper.readTree(modelResponse.getText());
        } catch (Exception ex) {
            return failure(result, "generate-sql", AskDataErrorCode.MODEL_RESPONSE_INVALID,
                    "AskData model response is not valid JSON");
        }
        String generatedSql = textValue(modelJson, "sql");
        if (isBlank(generatedSql)) {
            return failure(result, "generate-sql", AskDataErrorCode.MODEL_RESPONSE_INVALID,
                    "AskData model response does not contain SQL");
        }
        context.setSql(generatedSql);
        result.setTables(textValues(modelJson.get("tables")));
        result.setChartType(textValue(modelJson, "chartType"));
        result.setBrief(textValue(modelJson, "brief"));
        result.addStep("generate-sql", "Generate SQL", true, "SQL generated");

        AskDataDialect dialect = dialectRegistry.getDialect(dialect(request, metadata));
        AskDataSqlValidationResult validation;
        try {
            validation = sqlValidator.validate(generatedSql, dialect, maxRows);
        } catch (RuntimeException ex) {
            return failure(result, "validate-sql", AskDataErrorCode.SQL_REJECTED, message(ex));
        }
        if (validation == null || !validation.isValid()) {
            String errorCode = validation == null || isBlank(validation.getErrorCode()) ? AskDataErrorCode.SQL_REJECTED
                    : validation.getErrorCode();
            String errorMessage = validation == null ? "AskData SQL validation failed" : validation.getMessage();
            return failure(result, "validate-sql", errorCode, errorMessage);
        }
        result.setSql(validation.getSql());
        context.setSql(validation.getSql());
        result.addStep("validate-sql", "Validate SQL", true, "SQL validated");

        if (isExecutionEnabled(request)) {
            AskDataSqlExecutionResult execution;
            try {
                execution = sqlExecutor.execute(request, validation.getSql());
            } catch (RuntimeException ex) {
                return failure(result, "execute-sql", AskDataErrorCode.SQL_EXECUTION_FAILED, message(ex));
            }
            if (execution == null || !execution.isSuccess()) {
                String errorCode = execution == null || isBlank(execution.getErrorCode())
                        ? AskDataErrorCode.SQL_EXECUTION_FAILED : execution.getErrorCode();
                String errorMessage = execution == null ? "AskData SQL execution failed" : execution.getErrorMessage();
                return failure(result, "execute-sql", errorCode, errorMessage);
            }
            result.setColumns(execution.getColumns());
            result.setRows(execution.getRows());
            result.setRowCount(Integer.valueOf(execution.getRowCount()));
            result.addStep("execute-sql", "Execute SQL", true, "SQL executed");
        } else {
            result.addStep("execute-sql", "Execute SQL", true, "SQL execution skipped");
        }
        result.setSuccess(true);
        result.addStep("format-result", "Format Result", true, "Result formatted");
        return result;
    }

    private AskDataResult failure(AskDataResult result, String stepKey, String errorCode, String errorMessage) {
        result.setSuccess(false);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        result.addStep(stepKey, stepKey, false, errorMessage);
        return result;
    }

    private List<AskDataRetrievedDocument> retrieveDocuments(AskDataRequest request) {
        if (vectorRetriever == null) {
            return Collections.emptyList();
        }
        List<AskDataRetrievedDocument> documents = new ArrayList<AskDataRetrievedDocument>();
        String[] retrieveTypes = { "schema", "term", "example" };
        for (String retrieveType : retrieveTypes) {
            List<AskDataRetrievedDocument> retrieved = vectorRetriever.retrieve(new AskDataVectorQuery(retrieveType,
                    request == null ? null : request.getDatasourceKey(), request == null ? null : request.getDatasourceKey(),
                    request == null ? null : request.getQuestion(), topK(), Collections.<String, Object>emptyMap()));
            if (retrieved != null) {
                documents.addAll(retrieved);
            }
        }
        return documents;
    }

    private int maxRows(AskDataRequest request) {
        if (request != null && request.getMaxRows() != null && request.getMaxRows().intValue() > 0) {
            return request.getMaxRows().intValue();
        }
        Integer defaultMaxRows = properties.getSql().getDefaultMaxRows();
        return defaultMaxRows == null || defaultMaxRows.intValue() <= 0 ? 100 : defaultMaxRows.intValue();
    }

    private int topK() {
        Integer defaultTopK = properties.getVector().getDefaultTopK();
        return defaultTopK == null || defaultTopK.intValue() <= 0 ? 5 : defaultTopK.intValue();
    }

    private String dialect(AskDataRequest request, AskDataMetadata metadata) {
        if (request != null && !isBlank(request.getDialect())) {
            return request.getDialect();
        }
        return metadata.getDialect();
    }

    private boolean isExecutionEnabled(AskDataRequest request) {
        return properties.getSql().isExecuteEnabled() && request != null && Boolean.TRUE.equals(request.getExecute());
    }

    private List<String> textValues(JsonNode node) {
        List<String> values = new ArrayList<String>();
        if (node != null && node.isArray()) {
            for (JsonNode value : node) {
                if (value.isTextual()) {
                    values.add(value.asText());
                }
            }
        }
        return values;
    }

    private String textValue(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private String message(RuntimeException ex) {
        return isBlank(ex.getMessage()) ? ex.getClass().getSimpleName() : ex.getMessage();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
