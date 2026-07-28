package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.AskDataAutoConfiguration;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataMonitoringLog;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataMonitoringLogAccessPolicy;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataMonitoringLogStoreTest {

    @Test
    public void shouldPersistSanitizedLifecycleEventAndRequireProjectAdministratorForReading() {
        new ApplicationContextRunner()
                .withUserConfiguration(AskDataAutoConfiguration.class)
                .run(context -> {
                    AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-a");
                    AskDataEventPublisher publisher = context.getBean(AskDataEventPublisher.class);
                    AskDataMonitoringLogStore store = context.getBean(AskDataMonitoringLogStore.class);
                    AskDataMonitoringLogAccessPolicy accessPolicy =
                            context.getBean(AskDataMonitoringLogAccessPolicy.class);

                    publisher.publish(new AskDataEvent(owner, "trace-1", AskDataEvent.Type.CONVERSATION_STARTED,
                            "已脱敏的对话开始摘要"));

                    AskDataMonitoringLogStore.MonitoringLog stored = store.list(owner).get(0);
                    assertEquals("trace-1", stored.getTraceId());
                    assertEquals("CONVERSATION_STARTED", stored.getEventType());
                    AskDataMonitoringLog monitoringLog = new AskDataMonitoringLog(owner, stored.getTraceId(),
                            AskDataEvent.Type.valueOf(stored.getEventType()), stored.getMessage());
                    assertTrue(accessPolicy.canRead(new AskDataProjectContext("project-a", "admin-a"), true,
                            monitoringLog));
                    assertFalse(accessPolicy.canRead(owner, false, monitoringLog));
                    assertFalse(accessPolicy.canRead(new AskDataProjectContext("project-b", "admin-b"), true,
                            monitoringLog));
                });
    }

    @Test
    public void shouldRetainOnlyConfiguredNumberOfMonitoringLogsPerScope() {
        AskDataProjectContext context = new AskDataProjectContext("project-a", "operator-a");
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(1);
        store.append(context, new AskDataMonitoringLogStore.MonitoringLog("trace-1", "CONVERSATION_STARTED", "started"));
        store.append(context, new AskDataMonitoringLogStore.MonitoringLog("trace-2", "CONVERSATION_COMPLETED", "completed"));

        assertEquals(1, store.list(context).size());
        assertEquals("trace-2", store.list(context).get(0).getTraceId());
    }

    @Test
    public void shouldRejectDefaultMonitoringLogStoreInClusterModeWhenBusinessProvidesOtherSharedStores() {
        new ApplicationContextRunner()
                .withUserConfiguration(AskDataAutoConfiguration.class)
                .withPropertyValues("fw.plus.ai.askdata.state.cluster-mode=true")
                .withBean("customCoreConversationStore",
                        com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore.class,
                        () -> coreConversationStore())
                .withBean("customCoreCheckpointStore",
                        com.xiongx9527.fw.plus.ai.askdata.storage.AskDataCheckpointStore.class,
                        () -> coreCheckpointStore())
                .withBean("customCoreAgentStateStore",
                        com.xiongx9527.fw.plus.ai.askdata.storage.AskDataAgentStateStore.class,
                        () -> coreAgentStateStore())
                .withBean("customVectorRetriever",
                        com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector.AskDataVectorRetriever.class,
                        () -> query -> java.util.Collections.emptyList())
                .withBean("customLegacyConversationStore",
                        com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataConversationStore.class,
                        () -> legacyConversationStore())
                .run(context -> {
                    assertTrue(context.getStartupFailure() != null);
                    assertTrue(context.getStartupFailure().getMessage().contains("监控日志"));
                });
    }

    private com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore coreConversationStore() {
        return new com.xiongx9527.fw.plus.ai.askdata.storage.AskDataConversationStore() {
            @Override
            public void save(AskDataProjectContext context, String conversationId, Conversation conversation) {
            }

            @Override
            public Conversation load(AskDataProjectContext context, String conversationId) {
                return null;
            }
        };
    }

    private com.xiongx9527.fw.plus.ai.askdata.storage.AskDataCheckpointStore coreCheckpointStore() {
        return new com.xiongx9527.fw.plus.ai.askdata.storage.AskDataCheckpointStore() {
            @Override
            public Checkpoint save(AskDataProjectContext context, String checkpointId, Checkpoint checkpoint) {
                return checkpoint;
            }

            @Override
            public Checkpoint load(AskDataProjectContext context, String checkpointId) {
                return null;
            }
        };
    }

    private com.xiongx9527.fw.plus.ai.askdata.storage.AskDataAgentStateStore coreAgentStateStore() {
        return new com.xiongx9527.fw.plus.ai.askdata.storage.AskDataAgentStateStore() {
            @Override
            public void save(AskDataProjectContext context, String agentId, AgentState state) {
            }

            @Override
            public AgentState load(AskDataProjectContext context, String agentId) {
                return null;
            }
        };
    }

    private com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataConversationStore legacyConversationStore() {
        return new com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataConversationStore() {
            @Override
            public com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataCheckpoint save(
                    com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataCheckpoint checkpoint) {
                return checkpoint;
            }

            @Override
            public com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataCheckpoint loadLatest(
                    String conversationId) {
                return null;
            }

            @Override
            public com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation.AskDataCheckpoint loadByResumeToken(
                    String resumeToken) {
                return null;
            }
        };
    }
}
