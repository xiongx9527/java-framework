package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.ast.NebulaColumnDefinition;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaDdlDescriptor;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaDdlKind;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationDescriptor;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationKind;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NebulaBinlogEventFactoryTest {

	@Test
	public void forMutationShouldMapMutationKindsToRowEventTypesAndPayload() {
		assertMutationType(NebulaMutationKind.INSERT_VERTEX, NebulaBinlogEventType.WRITE_ROWS);
		assertMutationType(NebulaMutationKind.UPDATE_EDGE, NebulaBinlogEventType.UPDATE_ROWS);
		assertMutationType(NebulaMutationKind.DELETE_VERTEX, NebulaBinlogEventType.DELETE_ROWS);
	}

	@Test
	public void forDdlShouldBuildSchemaChangeEvent() {
		NebulaDdlDescriptor descriptor = new NebulaDdlDescriptor(
				NebulaDdlKind.CREATE_TAG,
				"person",
				Arrays.asList(
						new NebulaColumnDefinition("id", "string", true),
						new NebulaColumnDefinition("name", "string", false)
				),
				"CREATE TABLE person(id varchar(32) primary key, name varchar(32))",
				"CREATE TAG person(name string)"
		);

		NebulaBinlogEventDraft draft = NebulaBinlogEventFactory.forDdl(
				descriptor,
				"gateway-1",
				"trace-ddl",
				1234L
		);

		assertEquals(NebulaBinlogEventType.SCHEMA_CHANGE, draft.getEventType());
		assertEquals("gateway-1", draft.getSource());
		assertEquals("person", draft.getSchemaName());
		assertEquals(descriptor.getSourceSql(), draft.getSql());
		assertEquals(descriptor.getNgql(), draft.getNgql());
		assertEquals("CREATE_TAG", draft.getPayload().get("ddlKind"));
		assertEquals("person", draft.getPayload().get("schemaName"));
		assertEquals(descriptor.getColumns(), draft.getPayload().get("columns"));
		assertEquals("trace-ddl", draft.getPayload().get("traceId"));
		assertTrue(draft.getEventId().contains("trace-ddl"));
	}

	@Test
	public void factoryShouldRejectNullDescriptors() {
		try {
			NebulaBinlogEventFactory.forMutation(null, "gateway-1", "trace-1", 1L);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException exception) {
			assertTrue(exception.getMessage().contains("Mutation descriptor"));
		}
		try {
			NebulaBinlogEventFactory.forDdl(null, "gateway-1", "trace-1", 1L);
			fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException exception) {
			assertTrue(exception.getMessage().contains("DDL descriptor"));
		}
	}

	@Test
	public void factoryShouldRedactStatementTextAndPayloadValuesByDefault() {
		Map<String, Object> keys = new LinkedHashMap<String, Object>();
		keys.put("id", "person-secret");
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", "Sensitive Name");
		NebulaMutationDescriptor descriptor = new NebulaMutationDescriptor(
				NebulaMutationKind.INSERT_VERTEX,
				"person",
				keys,
				properties,
				"INSERT INTO person(id, name) VALUES ('person-secret', 'Sensitive Name')",
				"INSERT VERTEX person(name) VALUES \"person-secret\":(\"Sensitive Name\")"
		);

		NebulaBinlogEventDraft draft = NebulaBinlogEventFactory.forMutation(
				descriptor,
				"gateway-1",
				"trace-redacted",
				1000L
		);

		assertFalse(draft.getSql().contains("person-secret"));
		assertFalse(draft.getSql().contains("Sensitive Name"));
		assertFalse(draft.getNgql().contains("person-secret"));
		assertFalse(draft.getNgql().contains("Sensitive Name"));
		assertEquals("<redacted:string>", ((Map<?, ?>) draft.getPayload().get("keys")).get("id"));
		assertEquals("<redacted:string>", ((Map<?, ?>) draft.getPayload().get("properties")).get("name"));
		assertEquals("INSERT_VERTEX", draft.getPayload().get("mutationKind"));
		assertEquals("person", draft.getPayload().get("targetName"));
	}

	@Test
	public void factoryShouldAllowDisablingStatementTextAndPayloadValueCapture() {
		Map<String, Object> keys = new LinkedHashMap<String, Object>();
		keys.put("id", "person-secret");
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", "Sensitive Name");
		NebulaMutationDescriptor descriptor = new NebulaMutationDescriptor(
				NebulaMutationKind.INSERT_VERTEX,
				"person",
				keys,
				properties,
				"INSERT INTO person(id, name) VALUES ('person-secret', 'Sensitive Name')",
				"INSERT VERTEX person(name) VALUES \"person-secret\":(\"Sensitive Name\")"
		);

		NebulaBinlogEventDraft draft = NebulaBinlogEventFactory.forMutation(
				descriptor,
				"gateway-1",
				"trace-disabled",
				1000L,
				NebulaBinlogEventOptions.withCapture(false, false)
		);

		assertNull(draft.getSql());
		assertNull(draft.getNgql());
		assertEquals("<redacted:string>", ((Map<?, ?>) draft.getPayload().get("keys")).get("id"));
		assertEquals("<redacted:string>", ((Map<?, ?>) draft.getPayload().get("properties")).get("name"));
		assertEquals("INSERT_VERTEX", draft.getPayload().get("mutationKind"));
	}

	private void assertMutationType(NebulaMutationKind mutationKind, NebulaBinlogEventType eventType) {
		Map<String, Object> keys = new LinkedHashMap<String, Object>();
		keys.put("id", "p1");
		Map<String, Object> properties = new LinkedHashMap<String, Object>();
		properties.put("name", "Tom");
		NebulaMutationDescriptor descriptor = new NebulaMutationDescriptor(
				mutationKind,
				"person",
				keys,
				properties,
				"INSERT INTO person(id, name) VALUES ('p1', 'Tom')",
				"INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")"
		);

		NebulaBinlogEventDraft draft = NebulaBinlogEventFactory.forMutation(
				descriptor,
				"gateway-1",
				"trace-1",
				1000L
		);

		assertEquals(eventType, draft.getEventType());
		assertEquals("gateway-1", draft.getSource());
		assertEquals("person", draft.getSchemaName());
		assertEquals("INSERT INTO person(id, name) VALUES ('<redacted>', '<redacted>')", draft.getSql());
		assertEquals("INSERT VERTEX person(name) VALUES \"<redacted>\":(\"<redacted>\")", draft.getNgql());
		assertEquals(mutationKind.name(), draft.getPayload().get("mutationKind"));
		assertEquals("person", draft.getPayload().get("targetName"));
		assertEquals(Collections.singletonMap("id", (Object) "<redacted:string>"), draft.getPayload().get("keys"));
		assertEquals(Collections.singletonMap("name", (Object) "<redacted:string>"), draft.getPayload().get("properties"));
		assertEquals(Boolean.FALSE, draft.getPayload().get("beforeImageAvailable"));
		assertEquals("trace-1", draft.getPayload().get("traceId"));
		assertNotNull(draft.getEventId());
		assertFalse(draft.getEventId().trim().length() == 0);
	}
}
