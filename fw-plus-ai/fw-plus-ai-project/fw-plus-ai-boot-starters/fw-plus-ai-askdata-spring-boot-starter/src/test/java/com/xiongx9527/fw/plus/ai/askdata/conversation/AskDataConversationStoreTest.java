package com.xiongx9527.fw.plus.ai.askdata.conversation;

import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class AskDataConversationStoreTest {

    @Test
    public void shouldLoadLatestCheckpointAndResumeTokenWhilePreservingVersion() {
        AskDataConversationStore store = new InMemoryAskDataConversationStore();
        AskDataCheckpoint first = checkpoint("checkpoint-1", "resume-1", "1", 1000L);
        AskDataCheckpoint latest = checkpoint("checkpoint-2", "resume-2", "2", 2000L);

        store.save(first);
        store.save(latest);

        AskDataCheckpoint loadedLatest = store.loadLatest("conversation-1");
        assertSame(latest, loadedLatest);
        assertEquals("2", loadedLatest.getVersion());
        assertSame(first, store.loadByResumeToken("resume-1"));
        assertSame(latest, store.loadByResumeToken("resume-2"));
    }

    private AskDataCheckpoint checkpoint(String checkpointId, String resumeToken, String version, long createdAt) {
        AskDataCheckpoint checkpoint = new AskDataCheckpoint();
        checkpoint.setConversationId("conversation-1");
        checkpoint.setCheckpointId(checkpointId);
        checkpoint.setResumeToken(resumeToken);
        checkpoint.setVersion(version);
        checkpoint.setCreatedAt(new Date(createdAt));
        return checkpoint;
    }
}
