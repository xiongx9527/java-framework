package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;

public interface AskDataConversationStore {

    AskDataCheckpoint save(AskDataCheckpoint checkpoint);

    /**
     * 旧兼容读取入口，不携带调用者作用域。受保护流程必须使用带 {@link AskDataProjectContext} 的重载。
     */
    @Deprecated
    AskDataCheckpoint loadLatest(String conversationId);

    /**
     * 旧兼容读取入口，不携带调用者作用域。受保护流程必须使用带 {@link AskDataProjectContext} 的重载。
     */
    @Deprecated
    AskDataCheckpoint loadByResumeToken(String resumeToken);

    default AskDataCheckpoint save(AskDataProjectContext projectContext, AskDataCheckpoint checkpoint) {
        if (!isBoundTo(projectContext, checkpoint)) {
            throw new IllegalArgumentException("Checkpoint must be bound to project, operator and authorization version");
        }
        return save(checkpoint);
    }

    default AskDataCheckpoint loadLatest(AskDataProjectContext projectContext, String conversationId) {
        AskDataCheckpoint checkpoint = loadLatest(conversationId);
        return isBoundTo(projectContext, checkpoint) ? checkpoint : null;
    }

    default AskDataCheckpoint loadByResumeToken(AskDataProjectContext projectContext, String resumeToken) {
        AskDataCheckpoint checkpoint = loadByResumeToken(resumeToken);
        return isBoundTo(projectContext, checkpoint) ? checkpoint : null;
    }

    static boolean isBoundTo(AskDataProjectContext projectContext, AskDataCheckpoint checkpoint) {
        return projectContext != null && checkpoint != null
                && notBlank(projectContext.getProjectId())
                && notBlank(projectContext.getOperatorId())
                && projectContext.getProjectId().equals(checkpoint.getProjectId())
                && projectContext.getOperatorId().equals(checkpoint.getOperatorId())
                && notBlank(checkpoint.getAuthorizationVersion());
    }

    static boolean notBlank(String value) {
        return value != null && value.trim().length() > 0;
    }
}
