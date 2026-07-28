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
}
