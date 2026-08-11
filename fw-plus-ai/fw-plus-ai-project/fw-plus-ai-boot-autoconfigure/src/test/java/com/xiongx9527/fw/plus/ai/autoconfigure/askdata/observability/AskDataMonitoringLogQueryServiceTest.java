package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure.AskDataAutoConfiguration;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataMonitoringLogAccessPolicy;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAdministrativeScope;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.security.DefaultDenyAskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AskDataMonitoringLogQueryServiceTest {

    @Test
    public void shouldAllowProjectAdministratorToReadAllOperatorsInAuthorizedProject() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext ownerA = new AskDataProjectContext("project-a", "operator-a");
        AskDataProjectContext ownerB = new AskDataProjectContext("project-a", "operator-b");
        AskDataProjectContext administrator = new AskDataProjectContext("project-a", "admin-a");
        store.append(ownerA, new AskDataMonitoringLogStore.MonitoringLog("conversation-1", "COMPLETED", "done"));
        store.append(ownerB, new AskDataMonitoringLogStore.MonitoringLog("conversation-2", "COMPLETED", "done"));
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store,
                AskDataMonitoringLogAccessPolicy.projectAdministratorOnly(), projectAdministrator("admin-a"));

        List<AskDataMonitoringLogStore.MonitoringLog> result = service.queryProject(administrator, "project-a",
                AskDataMonitoringLogQuery.all());

        assertEquals(2, result.size());
        assertEquals("operator-a", result.get(0).getContext().getOperatorId());
        assertEquals("operator-b", result.get(1).getContext().getOperatorId());
        List<AskDataMonitoringLogStore.MonitoringLog> auditLogs = store.list(administrator);
        assertEquals(1, auditLogs.size());
        assertEquals("MONITORING_LOG_QUERY", auditLogs.get(0).getEventType());
        assertEquals("admin-a", auditLogs.get(0).getContext().getOperatorId());
        assertTrue(auditLogs.get(0).getMessage().contains("subject=project-a/admin-a"));
        assertTrue(auditLogs.get(0).getMessage().contains("scope=PROJECT:[project-a]"));
        assertTrue(auditLogs.get(0).getMessage().contains("resultCount=2"));
        assertTrue(auditLogs.get(0).getMessage().contains("authorizationSource=iam-main"));
        assertFalse(auditLogs.get(0).getMessage().contains("conversation-1"));
    }

    @Test
    public void shouldLimitPlatformAdministratorToGovernanceAuthorizedProjects() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext ownerA = new AskDataProjectContext("project-a", "operator-a");
        AskDataProjectContext ownerB = new AskDataProjectContext("project-b", "operator-b");
        AskDataProjectContext administrator = new AskDataProjectContext("platform", "platform-admin");
        store.append(ownerA, new AskDataMonitoringLogStore.MonitoringLog("conversation-a", "COMPLETED", "done"));
        store.append(ownerB, new AskDataMonitoringLogStore.MonitoringLog("conversation-b", "COMPLETED", "done"));
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store,
                AskDataMonitoringLogAccessPolicy.projectAdministratorOnly(),
                platformAdministratorFor("platform-admin", "project-a"));

        assertEquals(1, service.queryProject(administrator, "project-a",
                AskDataMonitoringLogQuery.all()).size());
        assertEquals(0, service.queryProject(administrator, "project-b",
                AskDataMonitoringLogQuery.all()).size());
    }

    @Test
    public void shouldRejectOrdinaryOperatorAndWriteSanitizedDenialAudit() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-a");
        AskDataProjectContext operator = new AskDataProjectContext("project-a", "operator-b");
        store.append(owner, new AskDataMonitoringLogStore.MonitoringLog("conversation-1", "COMPLETED", "done"));
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store,
                AskDataMonitoringLogAccessPolicy.projectAdministratorOnly(), request ->
                        AskDataAuthorizationSummary.deny(request, "NOT_ADMIN", "policy-v1", "iam-main"));

        assertEquals(0, service.queryProject(operator, "project-a", AskDataMonitoringLogQuery.all()).size());
        List<AskDataMonitoringLogStore.MonitoringLog> auditLogs = store.list(operator);
        assertEquals(1, auditLogs.size());
        assertEquals("GOVERNANCE_DENIED", auditLogs.get(0).getEventType());
        assertEquals(AskDataMonitoringLogQueryService.MONITORING_LOG_DATASOURCE,
                auditLogs.get(0).getDatasourceKey());
        assertEquals("NOT_ADMIN", auditLogs.get(0).getDenialCode());
        assertEquals("policy-v1", auditLogs.get(0).getAuthorizationVersion());
        assertEquals("iam-main", auditLogs.get(0).getAuthorizationSource());
        assertFalse(auditLogs.get(0).getMessage().contains("conversation-1"));
    }

    @Test
    public void shouldAuditSanitizedDenialWhenGovernanceProviderFails() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext administrator = new AskDataProjectContext("project-a", "admin-a");
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store,
                AskDataMonitoringLogAccessPolicy.projectAdministratorOnly(), request -> {
                    throw new IllegalStateException("credential=never-log-this");
                });

        assertEquals(0, service.queryProject(administrator, "project-a", AskDataMonitoringLogQuery.all()).size());

        AskDataMonitoringLogStore.MonitoringLog auditLog = store.list(administrator).get(0);
        assertEquals("GOVERNANCE_DENIED", auditLog.getEventType());
        assertEquals(DefaultDenyAskDataGovernanceProvider.DENIAL_CODE, auditLog.getDenialCode());
        assertFalse(auditLog.getAuthorizationVersion() == null
                || auditLog.getAuthorizationVersion().trim().isEmpty());
        assertFalse(auditLog.getAuthorizationSource() == null
                || auditLog.getAuthorizationSource().trim().isEmpty());
        assertFalse(auditLog.getMessage().contains("never-log-this"));
    }

    @Test
    public void shouldEvaluatePolicyAgainstImmutableLogOwner() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-b");
        AskDataProjectContext administrator = new AskDataProjectContext("project-a", "admin-a");
        store.append(owner, new AskDataMonitoringLogStore.MonitoringLog("conversation-1", "COMPLETED", "done"));
        AskDataMonitoringLogAccessPolicy ownerPolicy = (requester, administratorAllowed, log) ->
                administratorAllowed && "operator-b".equals(log.getContext().getOperatorId());
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store, ownerPolicy,
                projectAdministrator("admin-a"));

        assertEquals(1, service.queryProject(administrator, "project-a", AskDataMonitoringLogQuery.all()).size());
    }

    @Test
    public void shouldPreserveModelKeyWhenEvaluatingAccessPolicy() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-b");
        AskDataProjectContext administrator = new AskDataProjectContext("project-a", "admin-a");
        store.append(owner, new AskDataMonitoringLogStore.MonitoringLog(owner, "conversation-1", "MODEL_ROUTED",
                "model attempt", null, null, null, null, "EMBEDDING", "provider-embedding",
                "REQUEST_MODEL_KEY", "SUCCEEDED", null, null, "provider-embedding-v2"));
        AskDataMonitoringLogAccessPolicy modelPolicy = (requester, administratorAllowed, log) ->
                administratorAllowed && "EMBEDDING".equals(log.getModelType())
                        && "provider-embedding".equals(log.getModelKey())
                        && "REQUEST_MODEL_KEY".equals(log.getModelRoutePolicy())
                        && "SUCCEEDED".equals(log.getModelState())
                        && "provider-embedding-v2".equals(log.getActualModelKey());
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store, modelPolicy,
                projectAdministrator("admin-a"));

        assertEquals(1, service.queryProject(administrator, "project-a", AskDataMonitoringLogQuery.all()).size());
    }

    @Test
    public void shouldWireCoreGovernanceProviderFromAutoConfiguration() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataMonitoringLogQueryService service = new AskDataAutoConfiguration()
                .askDataMonitoringLogQueryService(store, AskDataMonitoringLogAccessPolicy.projectAdministratorOnly(),
                        projectAdministrator("admin-a"));

        assertEquals(0, service.queryProject(new AskDataProjectContext("project-a", "operator-a"), "project-a",
                AskDataMonitoringLogQuery.all()).size());
    }

    private AskDataGovernanceProvider projectAdministrator(final String operatorId) {
        return request -> {
            if (operatorId.equals(request.getProjectContext().getOperatorId())
                    && request.getAdministrativeScope().isProjectAdministrator()
                    && request.getAdministrativeScope().allowsProject(request.getProjectContext().getProjectId())) {
                return AskDataAuthorizationSummary.allow(request, Collections.emptyList(), Collections.emptyList(),
                        "policy-v1", "iam-main", request.getAdministrativeScope());
            }
            return AskDataAuthorizationSummary.deny(request, "NOT_ADMIN", "policy-v1", "iam-main");
        };
    }

    private AskDataGovernanceProvider platformAdministratorFor(final String operatorId,
            final String authorizedProjectId) {
        return request -> {
            AskDataAdministrativeScope scope = request.getAdministrativeScope();
            if (operatorId.equals(request.getProjectContext().getOperatorId()) && scope.isPlatformAdministrator()
                    && scope.allowsProject(authorizedProjectId)) {
                return AskDataAuthorizationSummary.allow(request, Collections.emptyList(), Collections.emptyList(),
                        "policy-v1", "iam-main", scope);
            }
            return AskDataAuthorizationSummary.deny(request, "PROJECT_NOT_AUTHORIZED", "policy-v1", "iam-main");
        };
    }
}
