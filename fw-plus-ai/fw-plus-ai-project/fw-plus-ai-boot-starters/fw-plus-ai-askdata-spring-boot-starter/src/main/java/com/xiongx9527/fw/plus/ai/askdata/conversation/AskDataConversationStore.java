package com.xiongx9527.fw.plus.ai.askdata.conversation;

public interface AskDataConversationStore {

    AskDataCheckpoint save(AskDataCheckpoint checkpoint);

    AskDataCheckpoint loadLatest(String conversationId);

    AskDataCheckpoint loadByResumeToken(String resumeToken);
}
