package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.AskDataAutoConfiguration;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataMonitoringLogAccessPolicy;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class AskDataMonitoringLogQueryServiceTest {

    @Test
    public void shouldRequireConfiguredPolicyForPlatformAdministrator() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-a");
        store.append(owner, new AskDataMonitoringLogStore.MonitoringLog("conversation-1", "COMPLETED", "done"));
        AskDataMonitoringLogAccessPolicy denyAllPolicy = (requester, projectAdministrator, log) -> false;
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store, denyAllPolicy);

        assertEquals(0, service.query(new AskDataProjectContext("platform", "admin"), owner,
                AskDataMonitoringLogAccessRole.PLATFORM_ADMINISTRATOR, AskDataMonitoringLogQuery.all()).size());
    }

    @Test
    public void shouldPassConfiguredPolicyFromAutoConfigurationToQueryService() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-a");
        store.append(owner, new AskDataMonitoringLogStore.MonitoringLog("conversation-1", "COMPLETED", "done"));
        AskDataMonitoringLogAccessPolicy denyAllPolicy = (requester, projectAdministrator, log) -> false;

        AskDataMonitoringLogQueryService service = new AskDataAutoConfiguration()
                .askDataMonitoringLogQueryService(store, denyAllPolicy);

        assertEquals(0, service.query(new AskDataProjectContext("project-a", "admin-a"), owner,
                AskDataMonitoringLogAccessRole.PROJECT_ADMINISTRATOR, AskDataMonitoringLogQuery.all()).size());
    }

    @Test
    public void shouldWriteSanitizedAuditLogWhenAdministratorReadsMonitoringLogs() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-a");
        AskDataProjectContext administrator = new AskDataProjectContext("project-a", "admin-a");
        store.append(owner, new AskDataMonitoringLogStore.MonitoringLog("conversation-1", "COMPLETED", "done"));
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store);

        assertEquals(1, service.query(administrator, owner, AskDataMonitoringLogAccessRole.PROJECT_ADMINISTRATOR,
                AskDataMonitoringLogQuery.byRequestId("conversation-1")).size());

        List<AskDataMonitoringLogStore.MonitoringLog> auditLogs = store.list(administrator);
        assertEquals(1, auditLogs.size());
        assertEquals("MONITORING_LOG_QUERY", auditLogs.get(0).getEventType());
        assertFalse(auditLogs.get(0).getMessage().contains("conversation-1"));
    }

    @Test
    public void shouldRequireAdministratorScopeForMonitoringLogs() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-a");
        store.append(owner, new AskDataMonitoringLogStore.MonitoringLog("conversation-1", "COMPLETED", "done"));
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store);

        assertEquals(1, service.query(new AskDataProjectContext("project-a", "admin-a"), owner,
                AskDataMonitoringLogAccessRole.PROJECT_ADMINISTRATOR,
                AskDataMonitoringLogQuery.byRequestId("conversation-1")).size());
        assertEquals(0, service.query(new AskDataProjectContext("platform", "admin"), owner,
                AskDataMonitoringLogAccessRole.PLATFORM_ADMINISTRATOR,
                AskDataMonitoringLogQuery.all()).size());
        assertEquals(0, service.query(new AskDataProjectContext("project-b", "admin-b"), owner,
                AskDataMonitoringLogAccessRole.PROJECT_ADMINISTRATOR, AskDataMonitoringLogQuery.all()).size());
        assertEquals(0, service.query(new AskDataProjectContext("project-a", "operator-b"), owner,
                AskDataMonitoringLogAccessRole.OPERATOR, AskDataMonitoringLogQuery.all()).size());
        assertEquals(0, service.query(owner, owner, AskDataMonitoringLogAccessRole.OPERATOR,
                AskDataMonitoringLogQuery.byConversationId("conversation-1")).size());
    }
}
