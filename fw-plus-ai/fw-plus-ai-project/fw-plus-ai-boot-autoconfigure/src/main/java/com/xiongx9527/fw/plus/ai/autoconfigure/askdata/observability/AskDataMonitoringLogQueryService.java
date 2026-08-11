package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataMonitoringLog;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataMonitoringLogAccessPolicy;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAdministrativeScope;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationPurpose;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationRequest;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizationSummary;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.security.DefaultDenyAskDataGovernanceProvider;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class AskDataMonitoringLogQueryService {

    public static final String MONITORING_LOG_DATASOURCE = "askdata-monitoring-log";

    private final AskDataMonitoringLogStore store;
    private final AskDataMonitoringLogAccessPolicy accessPolicy;
    private final AskDataGovernanceProvider governanceProvider;

    public AskDataMonitoringLogQueryService(AskDataMonitoringLogStore store) {
        this(store, AskDataMonitoringLogAccessPolicy.projectAdministratorOnly(),
                new DefaultDenyAskDataGovernanceProvider());
    }

    public AskDataMonitoringLogQueryService(AskDataMonitoringLogStore store,
            AskDataMonitoringLogAccessPolicy accessPolicy) {
        this(store, accessPolicy, new DefaultDenyAskDataGovernanceProvider());
    }

    public AskDataMonitoringLogQueryService(AskDataMonitoringLogStore store,
            AskDataMonitoringLogAccessPolicy accessPolicy, AskDataGovernanceProvider governanceProvider) {
        this.store = store;
        this.accessPolicy = accessPolicy;
        this.governanceProvider = governanceProvider;
    }

    public List<AskDataMonitoringLogStore.MonitoringLog> queryProject(AskDataProjectContext requester,
            String projectId, AskDataMonitoringLogQuery query) {
        if (requester == null || isBlank(projectId) || store == null || accessPolicy == null
                || governanceProvider == null) {
            return Collections.emptyList();
        }
        AskDataAdministrativeScope requestedScope = requester.getProjectId().equals(projectId)
                ? AskDataAdministrativeScope.project(projectId)
                : AskDataAdministrativeScope.platform(Collections.singleton(projectId));
        AskDataAuthorizationRequest authorizationRequest = new AskDataAuthorizationRequest(requester,
                MONITORING_LOG_DATASOURCE, AskDataAuthorizationPurpose.MONITORING_LOG_QUERY, requestedScope);
        AskDataAuthorizationSummary authorizationSummary;
        try {
            authorizationSummary = governanceProvider.authorize(authorizationRequest);
        } catch (RuntimeException ex) {
            auditDenied(requester, null, true);
            return Collections.emptyList();
        }
        if (authorizationSummary == null || !authorizationSummary.isAllowed()
                || !authorizationSummary.isBoundTo(authorizationRequest)
                || !authorizationSummary.getAdministrativeScope().allowsProject(projectId)) {
            auditDenied(requester, authorizationSummary, false);
            return Collections.emptyList();
        }
        List<AskDataMonitoringLogStore.MonitoringLog> result = new ArrayList<AskDataMonitoringLogStore.MonitoringLog>();
        for (AskDataMonitoringLogStore.MonitoringLog log : store.listProject(projectId)) {
            if (canRead(requester, authorizationSummary.getAdministrativeScope(), projectId, log)
                    && (query == null || isBlank(query.getTraceId()) || query.getTraceId().equals(log.getTraceId()))) {
                result.add(log);
            }
        }
        String auditMessage = "subject=" + requester.getProjectId() + "/" + requester.getOperatorId()
                + ",scope=" + authorizationSummary.getAdministrativeScope().getType() + ":"
                + authorizationSummary.getAdministrativeScope().getProjectIds() + ",resultCount=" + result.size()
                + ",authorizationSource=" + authorizationSummary.getAuthorizationSource()
                + ",traceFilter=" + traceFilterSummary(query);
        store.append(requester, new AskDataMonitoringLogStore.MonitoringLog(requester, null,
                "MONITORING_LOG_QUERY", auditMessage));
        return result;
    }

    private boolean canRead(AskDataProjectContext requester, AskDataAdministrativeScope administrativeScope,
            String projectId, AskDataMonitoringLogStore.MonitoringLog log) {
        if (log == null || log.getContext() == null || !projectId.equals(log.getContext().getProjectId())) {
            return false;
        }
        AskDataEvent.Type eventType;
        try {
            eventType = AskDataEvent.Type.valueOf(log.getEventType());
        } catch (RuntimeException ex) {
            eventType = AskDataEvent.Type.AGENT_NODE_EXECUTED;
        }
        AskDataMonitoringLog monitoringLog = new AskDataMonitoringLog(log.getContext(), log.getTraceId(),
                eventType, log.getMessage(), log.getDatasourceKey(), log.getDenialCode(),
                log.getAuthorizationVersion(), log.getAuthorizationSource(), log.getModelType(), log.getModelKey(),
                log.getModelRoutePolicy(), log.getModelState(), log.getModelFailureCode(),
                log.getModelSwitchReason(), log.getActualModelKey(), log.getCorrelation());
        return accessPolicy.canRead(requester, administrativeScope, monitoringLog);
    }

    private void auditDenied(AskDataProjectContext requester, AskDataAuthorizationSummary authorizationSummary,
            boolean providerError) {
        if (requester == null || store == null) {
            return;
        }
        String denialCode = authorizationSummary == null || isBlank(authorizationSummary.getDenialCode())
                ? DefaultDenyAskDataGovernanceProvider.DENIAL_CODE : authorizationSummary.getDenialCode();
        String version = providerError || authorizationSummary == null
                ? "governance-error-v1" : authorizationSummary.getAuthorizationVersion();
        String source = providerError || authorizationSummary == null
                ? "governance-provider-error" : authorizationSummary.getAuthorizationSource();
        AskDataEventPublisher.toStore(store).publish(AskDataEvent.governanceDenied(requester,
                UUID.randomUUID().toString(), MONITORING_LOG_DATASOURCE, denialCode, version, source));
    }

    private String traceFilterSummary(AskDataMonitoringLogQuery query) {
        return query == null || isBlank(query.getTraceId()) ? "all" : "present";
    }

    private boolean isBlank(String value) { return value == null || value.trim().length() == 0; }
}
