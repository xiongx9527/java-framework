package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AskDataMonitoringLogQueryService {

    private final AskDataMonitoringLogStore store;
    public AskDataMonitoringLogQueryService(AskDataMonitoringLogStore store) { this.store = store; }

    public List<AskDataMonitoringLogStore.MonitoringLog> query(AskDataProjectContext requester,
            AskDataProjectContext scope, AskDataMonitoringLogAccessRole role, AskDataMonitoringLogQuery query) {
        if (!canRead(requester, scope, role) || store == null) {
            return Collections.emptyList();
        }
        List<AskDataMonitoringLogStore.MonitoringLog> result = new ArrayList<AskDataMonitoringLogStore.MonitoringLog>();
        for (AskDataMonitoringLogStore.MonitoringLog log : store.list(scope)) {
            if (query == null || isBlank(query.getTraceId()) || query.getTraceId().equals(log.getTraceId())) {
                result.add(log);
            }
        }
        return result;
    }

    private boolean canRead(AskDataProjectContext requester, AskDataProjectContext scope, AskDataMonitoringLogAccessRole role) {
        if (requester == null || scope == null || role == null) { return false; }
        if (role == AskDataMonitoringLogAccessRole.PLATFORM_ADMINISTRATOR) { return true; }
        if (role == AskDataMonitoringLogAccessRole.PROJECT_ADMINISTRATOR) {
            return requester.getProjectId().equals(scope.getProjectId());
        }
        return false;
    }

    private boolean isBlank(String value) { return value == null || value.trim().length() == 0; }
}
