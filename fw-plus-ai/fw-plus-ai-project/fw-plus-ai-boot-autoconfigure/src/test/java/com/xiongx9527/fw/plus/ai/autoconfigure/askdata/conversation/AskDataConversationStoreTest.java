package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataResult;
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
import static org.junit.jupiter.api.Assertions.assertSame;
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
    public void shouldProvideRemainingCoreMemoryStateStoresWhenBusinessReplacesConversationStore() {
        com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore customConversationStore =
                new com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore() {
                    @Override
                    public void save(com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext context,
                            String conversationId, Conversation conversation) {
                    }

                    @Override
                    public Conversation load(com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext context,
                            String conversationId) {
                        return null;
                    }
                };

        new ApplicationContextRunner()
                .withUserConfiguration(AskDataAutoConfiguration.class)
                .withBean("customCoreConversationStore",
                        com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore.class,
                        () -> customConversationStore)
                .run(context -> {
                    assertTrue(context.getStartupFailure() == null);
                    assertSame(customConversationStore, context.getBean(
                            com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore.class));
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
    public void shouldRejectDisguisedStateAndVectorStoresWhenClusterModeIsEnabled() {
        new ApplicationContextRunner()
                .withUserConfiguration(AskDataAutoConfiguration.class)
                .withPropertyValues(
                        "fw.plus.ai.askdata.state.cluster-mode=true",
                        "fw.plus.ai.askdata.state.store=redis",
                        "fw.plus.ai.askdata.vector.store=redis")
                .run(context -> {
                    assertNotNull(context.getStartupFailure());
                    assertTrue(context.getStartupFailure().getMessage().contains("集群模式"));
                });
    }

    @Test
    public void shouldRetainOnlyConfiguredNumberOfCheckpointsPerConversation() {
        AskDataConversationStore store = new InMemoryAskDataConversationStore(1);
        store.save(checkpoint("checkpoint-1", "resume-1", "1", 1000L));
        store.save(checkpoint("checkpoint-2", "resume-2", "2", 2000L));

        assertEquals(null, store.loadByResumeToken("resume-1"));
        assertEquals("checkpoint-2", store.loadLatest("conversation-1").getCheckpointId());
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
        checkpoint.setActualEmbeddingModelKey("embedding-provider");
        checkpoint.setEmbeddingModelAttempts(Arrays.asList("embedding-primary", "embedding-provider"));
        AskDataResult child = new AskDataResult();
        child.setSuccess(true);
        child.setQuestionId("q1");
        child.setQuestionState("SUCCEEDED");
        child.setBrief("saved child");
        checkpoint.setMultiQuestion(Boolean.TRUE);
        checkpoint.setMultiQuestionPlanSnapshot("plan-v1");
        checkpoint.setChildResults(Arrays.asList(child));
        AskDataStepLog stepLog = new AskDataStepLog("metadata", "Metadata", true, "loaded");
        checkpoint.setSteps(new ArrayList<AskDataStepLog>(Arrays.asList(stepLog)));

        AskDataCheckpoint saved = store.save(checkpoint);
        checkpoint.setSql("select forged");
        checkpoint.getCompletedSteps().add("validate-sql");
        checkpoint.getSteps().get(0).setMessage("mutated");
        checkpoint.getChildResults().get(0).setBrief("mutated child");
        saved.getCompletedSteps().add("execute-sql");
        saved.getSteps().get(0).setSuccess(false);
        saved.getChildResults().get(0).setQuestionState("FAILED");

        AskDataCheckpoint loaded = store.loadByResumeToken("resume-1");
        assertEquals(null, loaded.getSql());
        assertEquals(Arrays.asList("metadata"), loaded.getCompletedSteps());
        assertEquals("embedding-provider", loaded.getActualEmbeddingModelKey());
        assertEquals(Arrays.asList("embedding-primary", "embedding-provider"),
                loaded.getEmbeddingModelAttempts());
        assertEquals("loaded", loaded.getSteps().get(0).getMessage());
        assertEquals(true, loaded.getSteps().get(0).isSuccess());
        assertEquals(Boolean.TRUE, loaded.getMultiQuestion());
        assertEquals("plan-v1", loaded.getMultiQuestionPlanSnapshot());
        assertEquals("saved child", loaded.getChildResults().get(0).getBrief());
        assertEquals("SUCCEEDED", loaded.getChildResults().get(0).getQuestionState());

        loaded.getCompletedSteps().add("generate-sql");
        loaded.getSteps().get(0).setMessage("changed-after-load");
        loaded.getChildResults().get(0).setBrief("changed-after-load");
        AskDataCheckpoint reloaded = store.loadByResumeToken("resume-1");
        assertEquals(Arrays.asList("metadata"), reloaded.getCompletedSteps());
        assertEquals("loaded", reloaded.getSteps().get(0).getMessage());
        assertEquals("saved child", reloaded.getChildResults().get(0).getBrief());
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
