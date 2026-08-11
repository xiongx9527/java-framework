package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataStepLog;

import java.util.ArrayList;
import java.util.Date;
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
