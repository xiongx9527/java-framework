package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.dify;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.MagicTinyflowKnowledgeAuditSink;
import com.xiongx9527.fw.plus.ai.tinyflow.knowledge.TinyflowKnowledgeAccessKeyRepository;

public final class TinyflowDifyExternalAuditCoordinator {

    private final MagicTinyflowKnowledgeAuditSink auditSink;

    public TinyflowDifyExternalAuditCoordinator(MagicTinyflowKnowledgeAuditSink auditSink) {
        if (auditSink == null) throw new IllegalArgumentException("Audit sink must not be null");
        this.auditSink = auditSink;
    }

    public void success(TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey, String knowledgeId) {
        record(accessKey, knowledgeId, true, null);
    }

    public void failure(TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey, String knowledgeId,
            RuntimeException error) {
        if (error == null) throw new IllegalArgumentException("External retrieval error must not be null");
        try {
            record(accessKey, knowledgeId, false, error);
        } catch (RuntimeException auditFailure) {
            if (auditFailure != error) error.addSuppressed(auditFailure);
        }
    }

    private void record(TinyflowKnowledgeAccessKeyRepository.AccessKey accessKey, String knowledgeId,
            boolean success, RuntimeException error) {
        try {
            auditSink.record(new MagicTinyflowKnowledgeAuditSink.AuditEvent(
                    accessKey == null ? null : accessKey.getTenantId(), "external:retrieval", knowledgeId,
                    success, error == null ? null : error.getClass().getName()));
        } catch (RuntimeException auditFailure) {
            if (error == null) throw new AuditPersistenceException(auditFailure);
            throw auditFailure;
        }
    }

    public static final class AuditPersistenceException extends IllegalStateException {
        private AuditPersistenceException(RuntimeException cause) {
            super("Tinyflow external audit persistence failed", cause);
        }
    }
}
