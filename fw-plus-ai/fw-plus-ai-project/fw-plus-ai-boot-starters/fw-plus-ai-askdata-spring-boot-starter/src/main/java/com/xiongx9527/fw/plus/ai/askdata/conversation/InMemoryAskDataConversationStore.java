package com.xiongx9527.fw.plus.ai.askdata.conversation;

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
        if (checkpoint.getCreatedAt() == null) {
            checkpoint.setCreatedAt(new Date());
        }
        List<AskDataCheckpoint> checkpoints = checkpointsByConversation.get(checkpoint.getConversationId());
        if (checkpoints == null) {
            List<AskDataCheckpoint> newCheckpoints = new ArrayList<AskDataCheckpoint>();
            List<AskDataCheckpoint> existing = checkpointsByConversation.putIfAbsent(checkpoint.getConversationId(),
                    newCheckpoints);
            checkpoints = existing == null ? newCheckpoints : existing;
        }
        synchronized (checkpoints) {
            checkpoints.add(checkpoint);
        }
        if (!isBlank(checkpoint.getResumeToken())) {
            checkpointsByResumeToken.put(checkpoint.getResumeToken(), checkpoint);
        }
        return checkpoint;
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
            return latest;
        }
    }

    @Override
    public AskDataCheckpoint loadByResumeToken(String resumeToken) {
        return isBlank(resumeToken) ? null : checkpointsByResumeToken.get(resumeToken);
    }

    private long createdAt(AskDataCheckpoint checkpoint) {
        Date createdAt = checkpoint.getCreatedAt();
        return createdAt == null ? Long.MIN_VALUE : createdAt.getTime();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
