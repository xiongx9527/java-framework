package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 按项目和操作人隔离、可配置容量的单机监控日志存储。
 */
public class InMemoryAskDataMonitoringLogStore implements AskDataMonitoringLogStore {

    private final int capacityPerScope;
    private final ConcurrentMap<ScopeKey, List<MonitoringLog>> logsByScope =
            new ConcurrentHashMap<ScopeKey, List<MonitoringLog>>();

    public InMemoryAskDataMonitoringLogStore(int capacityPerScope) {
        if (capacityPerScope <= 0) {
            throw new IllegalArgumentException("monitoringLogCapacity must be greater than zero");
        }
        this.capacityPerScope = capacityPerScope;
    }

    @Override
    public void append(AskDataProjectContext context, MonitoringLog log) {
        if (context == null || log == null) {
            throw new IllegalArgumentException("context and monitoringLog must not be null");
        }
        ScopeKey key = ScopeKey.of(context);
        List<MonitoringLog> logs = logsByScope.get(key);
        if (logs == null) {
            List<MonitoringLog> created = Collections.synchronizedList(new ArrayList<MonitoringLog>());
            List<MonitoringLog> existing = logsByScope.putIfAbsent(key, created);
            logs = existing == null ? created : existing;
        }
        synchronized (logs) {
            while (logs.size() >= capacityPerScope) {
                logs.remove(0);
            }
            logs.add(log.ownedBy(context));
        }
    }

    @Override
    public List<MonitoringLog> list(AskDataProjectContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        List<MonitoringLog> logs = logsByScope.get(ScopeKey.of(context));
        if (logs == null) {
            return Collections.emptyList();
        }
        synchronized (logs) {
            return new ArrayList<MonitoringLog>(logs);
        }
    }

    @Override
    public List<MonitoringLog> listProject(String projectId) {
        if (projectId == null || projectId.trim().length() == 0) {
            throw new IllegalArgumentException("projectId must not be blank");
        }
        List<ScopeKey> keys = new ArrayList<ScopeKey>();
        for (ScopeKey key : logsByScope.keySet()) {
            if (projectId.equals(key.projectId)) {
                keys.add(key);
            }
        }
        Collections.sort(keys, new Comparator<ScopeKey>() {
            @Override
            public int compare(ScopeKey left, ScopeKey right) {
                return left.operatorId.compareTo(right.operatorId);
            }
        });
        List<MonitoringLog> result = new ArrayList<MonitoringLog>();
        for (ScopeKey key : keys) {
            List<MonitoringLog> logs = logsByScope.get(key);
            if (logs != null) {
                synchronized (logs) {
                    result.addAll(logs);
                }
            }
        }
        return result;
    }

    private static final class ScopeKey {

        private final String projectId;
        private final String operatorId;

        private ScopeKey(String projectId, String operatorId) {
            this.projectId = projectId;
            this.operatorId = operatorId;
        }

        private static ScopeKey of(AskDataProjectContext context) {
            return new ScopeKey(context.getProjectId(), context.getOperatorId());
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ScopeKey)) {
                return false;
            }
            ScopeKey that = (ScopeKey) other;
            return projectId.equals(that.projectId) && operatorId.equals(that.operatorId);
        }

        @Override
        public int hashCode() {
            int result = projectId.hashCode();
            result = 31 * result + operatorId.hashCode();
            return result;
        }
    }
}
