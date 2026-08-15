package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicTinyflowKnowledgeAuditSink;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeAccessKeyRepository;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TinyflowDifyExternalAuditCoordinatorTest {

    @Test
    public void shouldRecordOnlySafeExternalRetrievalFields() {
        List<MagicTinyflowKnowledgeAuditSink.AuditEvent> events = new ArrayList<MagicTinyflowKnowledgeAuditSink.AuditEvent>();
        TinyflowDifyExternalAuditCoordinator coordinator =
                new TinyflowDifyExternalAuditCoordinator(events::add);

        coordinator.success(accessKey("tenant-a", "kb-1"), "kb-1");
        RuntimeException failure = new IllegalStateException("query=secret");
        coordinator.failure(accessKey("tenant-a", "kb-1"), "kb-1", failure);

        assertEquals(2, events.size());
        assertEquals("external:retrieval", events.get(0).getAction());
        assertEquals("tenant-a", events.get(0).getTenantId());
        assertEquals("kb-1", events.get(0).getResourceId());
        assertTrue(events.get(0).isSuccess());
        assertFalse(events.get(1).isSuccess());
        assertEquals(IllegalStateException.class.getName(), events.get(1).getErrorType());
        assertFalse(events.get(1).toString().contains("query=secret"));
    }

    @Test
    public void shouldFailClosedForSuccessfulBusinessWhenAuditPersistenceFails() {
        TinyflowDifyExternalAuditCoordinator coordinator =
                new TinyflowDifyExternalAuditCoordinator(event -> {
                    throw new IllegalStateException("audit unavailable");
                });

        assertThrows(TinyflowDifyExternalAuditCoordinator.AuditPersistenceException.class,
                () -> coordinator.success(accessKey("tenant-a", "kb-1"), "kb-1"));
    }

    @Test
    public void shouldKeepBusinessFailureAndAttachAuditFailure() {
        TinyflowDifyExternalAuditCoordinator coordinator =
                new TinyflowDifyExternalAuditCoordinator(event -> {
                    throw new IllegalStateException("audit unavailable");
                });
        RuntimeException business = new IllegalArgumentException("invalid request");

        coordinator.failure(accessKey("tenant-a", "kb-1"), "kb-1", business);

        assertEquals(1, business.getSuppressed().length);
        assertEquals("audit unavailable", business.getSuppressed()[0].getMessage());
    }

    private TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey(String tenantId, String knowledgeId) {
        return new TinyflowKnowledgeAccessKeyRepository.AccessKey("key-1", tenantId, knowledgeId,
                "PBKDF2WithHmacSHA256", "salt", 100000, "hash", true);
    }
}
