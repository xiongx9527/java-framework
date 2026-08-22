package com.xiongx9527.fw.plus.ai.tinyflow.vector;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Executable contract for durable compensation-safe vector store adapters. */
public abstract class AbstractTinyflowVectorStoreContractTest {

	private VectorStoreFixture fixture;

	protected abstract VectorStoreFixture createFixture();

	@Before
	public void setUpVectorStoreFixture() {
		fixture = createFixture();
	}

	@Test
	public void shouldRemoveTheSameExistingIdTwice() {
		fixture.addVector("vector-existing");
		assertTrue(fixture.containsVector("vector-existing"));

		fixture.getStore().removeAll(Collections.singletonList("vector-existing"));
		fixture.getStore().removeAll(Collections.singletonList("vector-existing"));

		assertFalse(fixture.containsVector("vector-existing"));
	}

	@Test
	public void shouldRemoveBatchContainingAlreadyMissingId() {
		fixture.addVector("vector-existing");
		assertTrue(fixture.containsVector("vector-existing"));

		fixture.getStore().removeAll(Arrays.asList("vector-existing", "vector-missing"));

		assertFalse(fixture.containsVector("vector-existing"));
		assertFalse(fixture.containsVector("vector-missing"));
	}

	@Test
	public void shouldConvergeAfterStageCasFailureWithNewCompensator() {
		fixture.addVector("vector-retry");
		assertTrue(fixture.containsVector("vector-retry"));
		StageRetryScenario scenario = fixture.createDeletePendingStageRetry("vector-retry");

		scenario.runUntilStageCasFails();

		assertFalse(fixture.containsVector("vector-retry"));
		assertTrue(scenario.isResourcePresent());
		assertTrue(scenario.isVectorStagePending());

		scenario.retryWithNewCompensator();

		assertFalse(fixture.containsVector("vector-retry"));
		assertFalse(scenario.isResourcePresent());
		assertFalse(scenario.isCompensationPresent());
		assertTrue(scenario.isDeletedTaskPersisted());
	}

	public interface VectorStoreFixture {
		TinyflowVectorStore getStore();
		void addVector(String id);
		boolean containsVector(String id);
		StageRetryScenario createDeletePendingStageRetry(String vectorId);
	}

	public interface StageRetryScenario {
		void runUntilStageCasFails();
		void retryWithNewCompensator();
		boolean isResourcePresent();
		boolean isVectorStagePending();
		boolean isCompensationPresent();
		boolean isDeletedTaskPersisted();
	}
}
