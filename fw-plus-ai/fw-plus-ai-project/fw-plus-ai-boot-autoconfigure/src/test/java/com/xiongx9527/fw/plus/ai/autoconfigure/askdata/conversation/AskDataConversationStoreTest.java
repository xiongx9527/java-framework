package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataStepLog;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.AskDataAutoConfiguration;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataAgentStateStore;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataCheckpointStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataConversationStoreTest {

    @Test
    public void shouldProvideCoreMemoryStateStoresByDefaultOnSingleNode() {
        new ApplicationContextRunner()
                .withUserConfiguration(AskDataAutoConfiguration.class)
                .run(context -> {
                    assertNotNull(context.getBean(com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore.class));
                    assertNotNull(context.getBean(AskDataCheckpointStore.class));
                    assertNotNull(context.getBean(AskDataAgentStateStore.class));
                });
    }

    @Test
    public void shouldRejectMemoryStateWhenClusterModeIsEnabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(AskDataAutoConfiguration.class)
                .withPropertyValues("fw.plus.ai.askdata.state.cluster-mode=true")
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(context.getStartupFailure().getMessage().contains("集群模式"));
                });
    }

    @Test
    public void shouldLoadLatestCheckpointAndResumeTokenWhilePreservingVersion() {
        AskDataConversationStore store = new InMemoryAskDataConversationStore();
        AskDataCheckpoint first = checkpoint("checkpoint-1", "resume-1", "1", 1000L);
        AskDataCheckpoint latest = checkpoint("checkpoint-2", "resume-2", "2", 2000L);

        store.save(first);
        store.save(latest);

        AskDataCheckpoint loadedLatest = store.loadLatest("conversation-1");
        assertNotSame(latest, loadedLatest);
        assertEquals("2", loadedLatest.getVersion());
        assertNotSame(first, store.loadByResumeToken("resume-1"));
        assertNotSame(latest, store.loadByResumeToken("resume-2"));
    }

    @Test
    public void shouldSnapshotCheckpointWhenSavingAndLoading() {
        AskDataConversationStore store = new InMemoryAskDataConversationStore();
        AskDataCheckpoint checkpoint = checkpoint("checkpoint-1", "resume-1", "1", 1000L);
        checkpoint.setCompletedSteps(new ArrayList<String>(Arrays.asList("metadata")));
        AskDataStepLog stepLog = new AskDataStepLog("metadata", "Metadata", true, "loaded");
        checkpoint.setSteps(new ArrayList<AskDataStepLog>(Arrays.asList(stepLog)));

        AskDataCheckpoint saved = store.save(checkpoint);
        checkpoint.setSql("select forged");
        checkpoint.getCompletedSteps().add("validate-sql");
        checkpoint.getSteps().get(0).setMessage("mutated");
        saved.getCompletedSteps().add("execute-sql");
        saved.getSteps().get(0).setSuccess(false);

        AskDataCheckpoint loaded = store.loadByResumeToken("resume-1");
        assertEquals(null, loaded.getSql());
        assertEquals(Arrays.asList("metadata"), loaded.getCompletedSteps());
        assertEquals("loaded", loaded.getSteps().get(0).getMessage());
        assertEquals(true, loaded.getSteps().get(0).isSuccess());

        loaded.getCompletedSteps().add("generate-sql");
        loaded.getSteps().get(0).setMessage("changed-after-load");
        AskDataCheckpoint reloaded = store.loadByResumeToken("resume-1");
        assertEquals(Arrays.asList("metadata"), reloaded.getCompletedSteps());
        assertEquals("loaded", reloaded.getSteps().get(0).getMessage());
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
