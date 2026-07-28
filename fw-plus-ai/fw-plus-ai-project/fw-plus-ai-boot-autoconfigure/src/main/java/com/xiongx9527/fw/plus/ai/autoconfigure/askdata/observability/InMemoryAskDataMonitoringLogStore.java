package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 按项目和操作人隔离、可配置容量的单机监控日志存储。
 */
public class InMemoryAskDataMonitoringLogStore implements AskDataMonitoringLogStore {

    private final int capacityPerScope;
    private final ConcurrentMap<String, List<MonitoringLog>> logsByScope =
            new ConcurrentHashMap<String, List<MonitoringLog>>();

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
        String key = context.getProjectId() + '\u0000' + context.getOperatorId();
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
            logs.add(log);
        }
    }

    @Override
    public List<MonitoringLog> list(AskDataProjectContext context) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        List<MonitoringLog> logs = logsByScope.get(context.getProjectId() + '\u0000' + context.getOperatorId());
        if (logs == null) {
            return Collections.emptyList();
        }
        synchronized (logs) {
            return new ArrayList<MonitoringLog>(logs);
        }
    }
}
