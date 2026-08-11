package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.storage.InMemoryAskDataStores;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CoreAskDataConversationStoreAdapterTest {

	@Test
	public void shouldPersistLegacyCheckpointThroughCoreStores() {
		InMemoryAskDataStores stores = InMemoryAskDataStores.singleNode();
		CoreAskDataConversationStoreAdapter adapter = new CoreAskDataConversationStoreAdapter(
				stores.conversations(), stores.checkpoints());
		AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-a");
		AskDataCheckpoint checkpoint = checkpoint();

		adapter.save(owner, checkpoint);

		assertEquals("checkpoint-a", adapter.loadLatest(owner, "conversation-a").getCheckpointId());
		assertEquals("checkpoint-a", adapter.loadByResumeToken(owner, "resume-a").getCheckpointId());
		assertNotNull(stores.checkpoints().load(owner, "conversation-a"));
		assertNull(adapter.loadLatest(new AskDataProjectContext("project-a", "operator-b"), "conversation-a"));
	}

	private static AskDataCheckpoint checkpoint() {
		AskDataCheckpoint checkpoint = new AskDataCheckpoint();
		checkpoint.setProjectId("project-a");
		checkpoint.setOperatorId("operator-a");
		checkpoint.setAuthorizationVersion("auth-v1");
		checkpoint.setConversationId("conversation-a");
		checkpoint.setCheckpointId("checkpoint-a");
		checkpoint.setResumeToken("resume-a");
		checkpoint.setCurrentStep("metadata");
		checkpoint.setVersion("1");
		checkpoint.setCreatedAt(new Date());
		return checkpoint;
	}
}
