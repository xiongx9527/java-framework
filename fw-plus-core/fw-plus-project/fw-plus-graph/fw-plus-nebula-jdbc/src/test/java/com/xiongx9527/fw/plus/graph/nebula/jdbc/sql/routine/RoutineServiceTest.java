package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.routine;

import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RoutineServiceTest {

	@Test
	public void createAndDropShouldManageDefinitions() throws Exception {
		InMemoryRoutineMetadataRepository repository = new InMemoryRoutineMetadataRepository();
		RoutineService service = new RoutineService(repository);

		service.create("graph_a", "alice",
				"CREATE PROCEDURE P(IN id VARCHAR(64)) BEGIN SELECT * FROM person WHERE id = id; END");

		assertNotNull(repository.find(RoutineName.of("graph_a", "p")));
		service.drop("graph_a", "alice", "DROP PROCEDURE p");
		assertNull(repository.find(RoutineName.of("graph_a", "p")));
	}

	@Test
	public void duplicateCreateShouldReturnStableMysqlError() throws Exception {
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository());
		String sql = "CREATE PROCEDURE p() BEGIN SELECT 1; END";
		service.create("graph_a", "alice", sql);

		try {
			service.create("graph_a", "alice", sql);
			fail("Duplicate create must fail");
		}
		catch (SQLException exception) {
			assertEquals(1304, exception.getErrorCode());
			assertEquals("42000", exception.getSQLState());
		}
	}

	@Test
	public void dropIfExistsShouldSucceedWhenMissing() throws Exception {
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository());

		assertEquals(0, service.drop("graph_a", "alice", "DROP PROCEDURE IF EXISTS p"));
	}

	@Test
	public void dropMissingWithoutIfExistsShouldReturnStableMysqlError() throws Exception {
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository());

		try {
			service.drop("graph_a", "alice", "DROP PROCEDURE p");
			fail("Missing drop must fail");
		}
		catch (SQLException exception) {
			assertEquals(1305, exception.getErrorCode());
			assertEquals("42000", exception.getSQLState());
			assertTrue(exception.getMessage().contains("p"));
		}
	}

	@Test
	public void dropShouldRequireRoutineManagePermissionForCurrentUser() throws Exception {
		RecordingPermissionChecker checker = new RecordingPermissionChecker();
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository(), 64,
				30000L, checker);
		service.create("graph_a", "creator", "CREATE PROCEDURE p() BEGIN SELECT 1; END");

		service.drop("graph_a", "dropper", "DROP PROCEDURE p");

		assertEquals("creator", checker.users.get(0));
		assertEquals(RoutinePermissionChecker.Action.MANAGE, checker.actions.get(0));
		assertEquals("dropper", checker.users.get(1));
		assertEquals(RoutinePermissionChecker.Action.MANAGE, checker.actions.get(1));
	}

	@Test
	public void createShouldRequireRoutineManagePermission() throws Exception {
		RoutineService service = new RoutineService(new InMemoryRoutineMetadataRepository(), 64,
				30000L, RoutinePermissionChecker.denyAll());

		try {
			service.create("graph_a", "alice", "CREATE PROCEDURE p() BEGIN SELECT 1; END");
			fail("CREATE PROCEDURE must require routine manage permission");
		}
		catch (SQLException exception) {
			assertEquals(1142, exception.getErrorCode());
			assertEquals("42000", exception.getSQLState());
		}
	}

	@Test
	public void callShouldRequireRoutineExecutePermission() throws Exception {
		InMemoryRoutineMetadataRepository repository = new InMemoryRoutineMetadataRepository();
		RoutineService service = new RoutineService(repository);
		service.create("graph_a", "alice", "CREATE PROCEDURE p() BEGIN SELECT 1; END");
		RoutineService denied = new RoutineService(repository, 64,
				30000L, RoutinePermissionChecker.denyAll());

		try {
			denied.call("graph_a", "bob", "CALL p()", null, null);
			fail("CALL must require routine execute permission");
		}
		catch (SQLException exception) {
			assertEquals(1142, exception.getErrorCode());
			assertEquals("42000", exception.getSQLState());
		}
	}

	private static class RecordingPermissionChecker implements RoutinePermissionChecker {
		private final List<String> users = new ArrayList<String>();
		private final List<Action> actions = new ArrayList<Action>();

		@Override
		public void check(String schema, String user, String routineName, Action action) {
			users.add(user);
			actions.add(action);
		}
	}
}
