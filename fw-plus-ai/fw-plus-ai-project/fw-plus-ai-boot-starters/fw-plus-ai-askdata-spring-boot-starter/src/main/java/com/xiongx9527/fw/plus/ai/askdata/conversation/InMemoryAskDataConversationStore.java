package com.xiongx9527.fw.plus.ai.askdata.conversation;

import com.xiongx9527.fw.plus.ai.askdata.AskDataStepLog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryAskDataConversationStore implements AskDataConversationStore {

    private final Map<String, List<AskDataCheckpoint>> checkpointsByConversation =
            new ConcurrentHashMap<String, List<AskDataCheckpoint>>();
    private final Map<String, AskDataCheckpoint> checkpointsByResumeToken =
            new ConcurrentHashMap<String, AskDataCheckpoint>();

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
        target.setDatasourceKey(source.getDatasourceKey());
        target.setDialect(source.getDialect());
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
