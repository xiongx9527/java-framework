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
    public void shouldRejectForgedProjectAdministratorRoleByDefault() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-a");
        AskDataProjectContext requester = new AskDataProjectContext("project-a", "operator-b");
        store.append(owner, new AskDataMonitoringLogStore.MonitoringLog("conversation-1", "COMPLETED", "done"));
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store);

        assertEquals(0, service.query(requester, owner, AskDataMonitoringLogAccessRole.PROJECT_ADMINISTRATOR,
                AskDataMonitoringLogQuery.all()).size());
        assertEquals(0, store.list(requester).size());
    }

    @Test
    public void shouldRequireConfiguredPolicyForPlatformAdministrator() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-a");
        store.append(owner, new AskDataMonitoringLogStore.MonitoringLog("conversation-1", "COMPLETED", "done"));
        AskDataMonitoringLogAccessPolicy denyAllPolicy = (requester, projectAdministrator, log) -> false;
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store, denyAllPolicy,
                (requester, scope, role) -> true);

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
                .askDataMonitoringLogQueryService(store, denyAllPolicy, (requester, scope, role) -> true);

        assertEquals(0, service.query(new AskDataProjectContext("project-a", "admin-a"), owner,
                AskDataMonitoringLogAccessRole.PROJECT_ADMINISTRATOR, AskDataMonitoringLogQuery.all()).size());
    }

    @Test
    public void shouldWriteSanitizedAuditLogWhenAdministratorReadsMonitoringLogs() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-a");
        AskDataProjectContext administrator = new AskDataProjectContext("project-a", "admin-a");
        store.append(owner, new AskDataMonitoringLogStore.MonitoringLog("conversation-1", "COMPLETED", "done"));
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store,
                AskDataMonitoringLogAccessPolicy.projectAdministratorOnly(),
                (requester, scope, role) -> role == AskDataMonitoringLogAccessRole.PROJECT_ADMINISTRATOR
                        && requester.getProjectId().equals(scope.getProjectId()));

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
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store,
                AskDataMonitoringLogAccessPolicy.projectAdministratorOnly(),
                (requester, scope, role) -> role == AskDataMonitoringLogAccessRole.PROJECT_ADMINISTRATOR
                        && requester.getProjectId().equals(scope.getProjectId()));

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

    @Test
    public void shouldAllowExplicitlyAuthorizedProjectAdministratorOnlyWithinOwnProject() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-a");
        AskDataProjectContext administrator = new AskDataProjectContext("project-a", "admin-a");
        AskDataProjectContext otherProjectOwner = new AskDataProjectContext("project-b", "operator-b");
        store.append(owner, new AskDataMonitoringLogStore.MonitoringLog("conversation-1", "COMPLETED", "done"));
        store.append(otherProjectOwner,
                new AskDataMonitoringLogStore.MonitoringLog("conversation-2", "COMPLETED", "done"));
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store,
                AskDataMonitoringLogAccessPolicy.projectAdministratorOnly(),
                (requester, scope, role) -> role == AskDataMonitoringLogAccessRole.PROJECT_ADMINISTRATOR
                        && requester.getOperatorId().equals("admin-a")
                        && requester.getProjectId().equals(scope.getProjectId()));

        assertEquals(1, service.query(administrator, owner, AskDataMonitoringLogAccessRole.PROJECT_ADMINISTRATOR,
                AskDataMonitoringLogQuery.all()).size());
        assertEquals(0, service.query(administrator, otherProjectOwner,
                AskDataMonitoringLogAccessRole.PROJECT_ADMINISTRATOR, AskDataMonitoringLogQuery.all()).size());
        assertEquals(1, store.list(administrator).size());
    }

    @Test
    public void shouldRequireExplicitPlatformAuthorizationAndRespectScopeAndPolicy() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext projectAOwner = new AskDataProjectContext("project-a", "operator-a");
        AskDataProjectContext projectBOwner = new AskDataProjectContext("project-b", "operator-b");
        AskDataProjectContext platformAdministrator = new AskDataProjectContext("platform", "platform-admin");
        store.append(projectAOwner, new AskDataMonitoringLogStore.MonitoringLog("conversation-a", "COMPLETED", "done"));
        store.append(projectBOwner, new AskDataMonitoringLogStore.MonitoringLog("conversation-b", "COMPLETED", "done"));
        AskDataMonitoringLogAccessPolicy projectAPolicy = (requester, projectAdministrator, log) -> projectAdministrator
                && "project-a".equals(log.getContext().getProjectId());
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store, projectAPolicy,
                (requester, scope, role) -> role == AskDataMonitoringLogAccessRole.PLATFORM_ADMINISTRATOR
                        && requester.getOperatorId().equals("platform-admin")
                        && "project-a".equals(scope.getProjectId()));

        assertEquals(1, service.query(platformAdministrator, projectAOwner,
                AskDataMonitoringLogAccessRole.PLATFORM_ADMINISTRATOR, AskDataMonitoringLogQuery.all()).size());
        assertEquals(0, service.query(platformAdministrator, null, AskDataMonitoringLogAccessRole.PLATFORM_ADMINISTRATOR,
                AskDataMonitoringLogQuery.all()).size());
        assertEquals(0, service.query(platformAdministrator, projectBOwner,
                AskDataMonitoringLogAccessRole.PLATFORM_ADMINISTRATOR, AskDataMonitoringLogQuery.all()).size());
        assertEquals(0, service.query(new AskDataProjectContext("platform", "operator-x"), projectAOwner,
                AskDataMonitoringLogAccessRole.PLATFORM_ADMINISTRATOR, AskDataMonitoringLogQuery.all()).size());
        assertEquals(1, store.list(platformAdministrator).size());
    }
}
