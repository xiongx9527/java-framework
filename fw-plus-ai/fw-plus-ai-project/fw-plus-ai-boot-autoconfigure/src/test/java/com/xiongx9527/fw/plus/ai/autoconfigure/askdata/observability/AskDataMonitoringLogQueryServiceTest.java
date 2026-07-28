package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.storage.AskDataMonitoringLogStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AskDataMonitoringLogQueryServiceTest {

    @Test
    public void shouldQueryByConversationOrRequestAndApplyRoles() {
        InMemoryAskDataMonitoringLogStore store = new InMemoryAskDataMonitoringLogStore(10);
        AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-a");
        store.append(owner, new AskDataMonitoringLogStore.MonitoringLog("conversation-1", "COMPLETED", "done"));
        AskDataMonitoringLogQueryService service = new AskDataMonitoringLogQueryService(store);

        assertEquals(1, service.query(owner, owner, AskDataMonitoringLogAccessRole.OPERATOR,
                AskDataMonitoringLogQuery.byConversationId("conversation-1")).size());
        assertEquals(1, service.query(new AskDataProjectContext("project-a", "admin-a"), owner,
                AskDataMonitoringLogAccessRole.PROJECT_ADMINISTRATOR,
                AskDataMonitoringLogQuery.byRequestId("conversation-1")).size());
        assertEquals(1, service.query(new AskDataProjectContext("platform", "admin"), owner,
                AskDataMonitoringLogAccessRole.PLATFORM_ADMINISTRATOR,
                AskDataMonitoringLogQuery.all()).size());
        assertEquals(0, service.query(new AskDataProjectContext("project-a", "operator-b"), owner,
                AskDataMonitoringLogAccessRole.OPERATOR, AskDataMonitoringLogQuery.all()).size());
    }
}
