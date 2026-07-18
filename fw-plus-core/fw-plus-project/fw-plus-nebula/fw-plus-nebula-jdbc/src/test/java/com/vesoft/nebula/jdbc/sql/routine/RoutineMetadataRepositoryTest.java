package com.vesoft.nebula.jdbc.sql.routine;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RoutineMetadataRepositoryTest {

	@Test
	public void saveFindAndDropRoutineDefinition() throws Exception {
		InMemoryRoutineMetadataRepository repository = new InMemoryRoutineMetadataRepository();
		RoutineDefinition definition = definition("graph_a", "P");

		repository.save(definition);

		RoutineDefinition found = repository.find(RoutineName.of("graph_a", "p"));
		assertNotNull(found);
		assertEquals("graph_a", found.getSchema());
		assertEquals("P", found.getName());
		assertEquals("p", found.getNormalizedName());
		assertEquals("alice", found.getOwner());
		assertEquals(1, found.getParameters().size());
		assertEquals(1, found.getBodyStatements().size());
		assertTrue(repository.drop(RoutineName.of("graph_a", "P")));
		assertNull(repository.find(RoutineName.of("graph_a", "p")));
		assertFalse(repository.drop(RoutineName.of("graph_a", "p")));
	}

	@Test
	public void schemaIsPartOfRoutineKey() throws Exception {
		InMemoryRoutineMetadataRepository repository = new InMemoryRoutineMetadataRepository();
		repository.save(definition("graph_a", "P"));
		repository.save(definition("graph_b", "P"));

		assertEquals("graph_a", repository.find(RoutineName.of("graph_a", "p")).getSchema());
		assertEquals("graph_b", repository.find(RoutineName.of("graph_b", "p")).getSchema());
	}

	private RoutineDefinition definition(String schema, String name) throws Exception {
		RoutineStatement statement = RoutineStatementParser.parse(
				"CREATE PROCEDURE " + name + "(IN id VARCHAR(64)) BEGIN SELECT * FROM person WHERE id = id; END");
		return RoutineDefinition.fromStatement(schema, "alice", statement);
	}
}
