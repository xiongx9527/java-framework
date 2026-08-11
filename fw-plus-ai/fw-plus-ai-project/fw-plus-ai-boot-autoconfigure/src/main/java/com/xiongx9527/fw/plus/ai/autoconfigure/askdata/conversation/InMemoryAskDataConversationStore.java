package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataColumn;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataStepLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAskDataConversationStore implements AskDataConversationStore {

    private static final int DEFAULT_MAX_CHECKPOINTS_PER_CONVERSATION = 100;

    private final int maxCheckpointsPerConversation;
    private final Map<String, List<AskDataCheckpoint>> checkpointsByConversation =
            new ConcurrentHashMap<String, List<AskDataCheckpoint>>();
    private final Map<String, AskDataCheckpoint> checkpointsByResumeToken =
            new ConcurrentHashMap<String, AskDataCheckpoint>();

    public InMemoryAskDataConversationStore() {
        this(DEFAULT_MAX_CHECKPOINTS_PER_CONVERSATION);
    }

    public InMemoryAskDataConversationStore(int maxCheckpointsPerConversation) {
        if (maxCheckpointsPerConversation <= 0) {
            throw new IllegalArgumentException("maxCheckpointsPerConversation must be greater than zero");
        }
        this.maxCheckpointsPerConversation = maxCheckpointsPerConversation;
    }

    @Override
    public AskDataCheckpoint save(AskDataCheckpoint checkpoint) {
        if (checkpoint == null || isBlank(checkpoint.getConversationId())) {
            throw new IllegalArgumentException("Checkpoint conversationId must not be blank");
        }
        AskDataCheckpoint stored = snapshot(checkpoint);
        if (stored.getCreatedAt() == null) {
            stored.setCreatedAt(new Date());
        }
        List<AskDataCheckpoint> checkpoints = checkpointsByConversation.get(stored.getConversationId());
        if (checkpoints == null) {
            List<AskDataCheckpoint> newCheckpoints = new ArrayList<AskDataCheckpoint>();
            List<AskDataCheckpoint> existing = checkpointsByConversation.putIfAbsent(stored.getConversationId(),
                    newCheckpoints);
            checkpoints = existing == null ? newCheckpoints : existing;
        }
        synchronized (checkpoints) {
            checkpoints.add(stored);
            while (checkpoints.size() > maxCheckpointsPerConversation) {
                AskDataCheckpoint evicted = checkpoints.remove(0);
                if (!isBlank(evicted.getResumeToken())) {
                    checkpointsByResumeToken.remove(evicted.getResumeToken(), evicted);
                }
            }
        }
        if (!isBlank(stored.getResumeToken())) {
            checkpointsByResumeToken.put(stored.getResumeToken(), stored);
        }
        return snapshot(stored);
    }

    @Override
    public AskDataCheckpoint loadLatest(String conversationId) {
        List<AskDataCheckpoint> checkpoints = checkpointsByConversation.get(conversationId);
        if (checkpoints == null) {
            return null;
        }
        synchronized (checkpoints) {
            AskDataCheckpoint latest = null;
            for (AskDataCheckpoint checkpoint : checkpoints) {
                if (latest == null || createdAt(checkpoint) >= createdAt(latest)) {
                    latest = checkpoint;
                }
            }
            return latest == null ? null : snapshot(latest);
        }
    }

    @Override
    public AskDataCheckpoint loadByResumeToken(String resumeToken) {
        if (isBlank(resumeToken)) {
            return null;
        }
        AskDataCheckpoint checkpoint = checkpointsByResumeToken.get(resumeToken);
        return checkpoint == null ? null : snapshot(checkpoint);
    }

    private AskDataCheckpoint snapshot(AskDataCheckpoint source) {
        AskDataCheckpoint target = new AskDataCheckpoint();
        target.setConversationId(source.getConversationId());
        target.setCheckpointId(source.getCheckpointId());
        target.setResumeToken(source.getResumeToken());
        target.setRequestSummary(source.getRequestSummary());
        target.setModelKey(source.getModelKey());
        target.setActualModelKey(source.getActualModelKey());
        target.setModelAttempts(source.getModelAttempts());
        target.setActualEmbeddingModelKey(source.getActualEmbeddingModelKey());
        target.setEmbeddingModelAttempts(source.getEmbeddingModelAttempts());
        target.setMultiQuestion(source.getMultiQuestion());
        target.setMultiQuestionPlanSnapshot(source.getMultiQuestionPlanSnapshot());
        target.setChildResults(copyResults(source.getChildResults()));
        target.setDatasourceKey(source.getDatasourceKey());
        target.setDialect(source.getDialect());
        target.setMaxRows(source.getMaxRows());
        target.setExecute(source.getExecute());
        target.setRequestOptionsSnapshot(source.getRequestOptionsSnapshot());
        target.setExecutionState(source.getExecutionState());
        target.setCompletedSteps(source.getCompletedSteps());
        target.setCurrentStep(source.getCurrentStep());
        target.setSql(source.getSql());
        target.setSqlValidated(source.getSqlValidated());
        target.setSqlExecuted(source.getSqlExecuted());
        target.setRowCount(source.getRowCount());
        target.setCompressedSummary(source.getCompressedSummary());
        target.setSteps(copyStepLogs(source.getSteps()));
        target.setErrorCode(source.getErrorCode());
        target.setVersion(source.getVersion());
        target.setCreatedAt(source.getCreatedAt());
        return target;
    }

    private List<AskDataResult> copyResults(List<AskDataResult> source) {
        List<AskDataResult> target = new ArrayList<AskDataResult>();
        if (source != null) {
            for (AskDataResult result : source) {
                if (result != null) {
                    target.add(copyResult(result));
                }
            }
        }
        return target;
    }

    private AskDataResult copyResult(AskDataResult source) {
        AskDataResult target = new AskDataResult();
        target.setSuccess(source.isSuccess());
        target.setErrorCode(source.getErrorCode());
        target.setErrorMessage(source.getErrorMessage());
        target.setSql(source.getSql());
        target.setTables(new ArrayList<String>(source.getTables()));
        target.setChartType(source.getChartType());
        target.setBrief(source.getBrief());
        target.setColumns(copyColumns(source.getColumns()));
        target.setRows(copyRows(source.getRows()));
        target.setRowCount(source.getRowCount());
        target.setSteps(copyStepLogs(source.getSteps()));
        target.setConversationId(source.getConversationId());
        target.setResumeToken(source.getResumeToken());
        target.setCheckpointId(source.getCheckpointId());
        target.setActualModelKey(source.getActualModelKey());
        target.setModelAttempts(source.getModelAttempts());
        target.setActualEmbeddingModelKey(source.getActualEmbeddingModelKey());
        target.setEmbeddingModelAttempts(source.getEmbeddingModelAttempts());
        target.setQuestionId(source.getQuestionId());
        target.setQuestionState(source.getQuestionState());
        target.setDependsOn(source.getDependsOn());
        target.setChildResults(copyResults(source.getChildResults()));
        return target;
    }

    private List<AskDataColumn> copyColumns(List<AskDataColumn> source) {
        List<AskDataColumn> target = new ArrayList<AskDataColumn>();
        if (source != null) {
            for (AskDataColumn column : source) {
                if (column != null) {
                    AskDataColumn copied = new AskDataColumn();
                    copied.setName(column.getName());
                    copied.setLabel(column.getLabel());
                    copied.setType(column.getType());
                    target.add(copied);
                }
            }
        }
        return target;
    }

    private List<Map<String, Object>> copyRows(List<Map<String, Object>> source) {
        List<Map<String, Object>> target = new ArrayList<Map<String, Object>>();
        if (source != null) {
            for (Map<String, Object> row : source) {
                target.add(row == null ? null : new LinkedHashMap<String, Object>(row));
            }
        }
        return target;
    }

    private List<AskDataStepLog> copyStepLogs(List<AskDataStepLog> source) {
        List<AskDataStepLog> target = new ArrayList<AskDataStepLog>();
        if (source != null) {
            for (AskDataStepLog step : source) {
                if (step != null) {
                    target.add(new AskDataStepLog(step.getStepKey(), step.getStepName(), step.isSuccess(),
                            step.getMessage()));
                }
            }
        }
        return target;
    }

    private long createdAt(AskDataCheckpoint checkpoint) {
        Date createdAt = checkpoint.getCreatedAt();
        return createdAt == null ? Long.MIN_VALUE : createdAt.getTime();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
