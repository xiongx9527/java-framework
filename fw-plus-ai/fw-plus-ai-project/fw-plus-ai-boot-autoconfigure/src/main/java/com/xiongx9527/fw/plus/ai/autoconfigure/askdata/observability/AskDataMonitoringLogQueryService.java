package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataMonitoringLog;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataMonitoringLogAccessPolicy;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AskDataMonitoringLogQueryService {

    private final AskDataMonitoringLogStore store;
    private final AskDataMonitoringLogAccessPolicy accessPolicy;
    private final AskDataMonitoringLogAccessAuthorizer accessAuthorizer;

    public AskDataMonitoringLogQueryService(AskDataMonitoringLogStore store) {
        this(store, AskDataMonitoringLogAccessPolicy.projectAdministratorOnly(),
                AskDataMonitoringLogAccessAuthorizer.denyAll());
    }

    public AskDataMonitoringLogQueryService(AskDataMonitoringLogStore store,
            AskDataMonitoringLogAccessPolicy accessPolicy) {
        this(store, accessPolicy, AskDataMonitoringLogAccessAuthorizer.denyAll());
    }

    public AskDataMonitoringLogQueryService(AskDataMonitoringLogStore store,
            AskDataMonitoringLogAccessPolicy accessPolicy, AskDataMonitoringLogAccessAuthorizer accessAuthorizer) {
        this.store = store;
        this.accessPolicy = accessPolicy;
        this.accessAuthorizer = accessAuthorizer;
    }

    public List<AskDataMonitoringLogStore.MonitoringLog> query(AskDataProjectContext requester,
            AskDataProjectContext scope, AskDataMonitoringLogAccessRole role, AskDataMonitoringLogQuery query) {
        if (requester == null || scope == null || role == null || store == null || accessPolicy == null
                || accessAuthorizer == null) {
            return Collections.emptyList();
        }
        boolean projectAdministrator = accessAuthorizer.isAdministrator(requester, scope, role);
        if (!projectAdministrator) {
            return Collections.emptyList();
        }
        List<AskDataMonitoringLogStore.MonitoringLog> result = new ArrayList<AskDataMonitoringLogStore.MonitoringLog>();
        for (AskDataMonitoringLogStore.MonitoringLog log : store.list(scope)) {
            if (canRead(requester, scope, projectAdministrator, log)
                    && (query == null || isBlank(query.getTraceId()) || query.getTraceId().equals(log.getTraceId()))) {
                result.add(log);
            }
        }
        store.append(requester, new AskDataMonitoringLogStore.MonitoringLog(null, "MONITORING_LOG_QUERY",
                "scopeProject=" + scope.getProjectId() + ",traceFilter=" + traceFilterSummary(query)));
        return result;
    }

    private boolean canRead(AskDataProjectContext requester, AskDataProjectContext scope, boolean projectAdministrator,
            AskDataMonitoringLogStore.MonitoringLog log) {
        AskDataMonitoringLog monitoringLog = new AskDataMonitoringLog(scope, log.getTraceId(),
                AskDataEvent.Type.AGENT_NODE_EXECUTED, log.getMessage());
        return accessPolicy.canRead(requester, projectAdministrator, monitoringLog);
    }

    private String traceFilterSummary(AskDataMonitoringLogQuery query) {
        return query == null || isBlank(query.getTraceId()) ? "all" : "present";
    }

    private boolean isBlank(String value) { return value == null || value.trim().length() == 0; }
}
