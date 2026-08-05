package com.xiongx9527.fw.plus.ai.tinyflow.workflow;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import dev.tinyflow.core.chain.ChainDefinition;
import dev.tinyflow.core.parser.ChainParser;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TinyflowWorkflowValidatorTest {

	private TinyflowWorkflowValidator validator;

	@Before
	public void setUp() {
		validator = new TinyflowWorkflowValidator(ChainParser.builder().build());
	}

	@Test
	public void shouldRejectMalformedJsonWithResourceId() {
		TinyflowException error = assertThrows(TinyflowException.class,
				() -> validator.validate("customer/order", "{"));

		assertTrue(error.getMessage().contains("customer/order"));
	}

	@Test
	public void shouldRejectMissingAndEmptyNodeArrays() {
		TinyflowException missing = assertThrows(TinyflowException.class,
				() -> validator.validate("customer/missing", "{\"edges\":[]}"));
		TinyflowException empty = assertThrows(TinyflowException.class,
				() -> validator.validate("customer/empty", "{\"nodes\":[],\"edges\":[]}"));

		assertTrue(missing.getMessage().contains("customer/missing"));
		assertTrue(missing.getMessage().contains("nodes"));
		assertTrue(empty.getMessage().contains("customer/empty"));
		assertTrue(empty.getMessage().contains("nodes"));
	}

	@Test
	public void shouldRejectUnknownNodeBeforeTinyflowParserSilentlyDropsIt() {
		String json = "{\"nodes\":[{\"id\":\"n1\",\"type\":\"missing-node\"}],\"edges\":[]}";
		TinyflowException error = assertThrows(TinyflowException.class,
				() -> validator.validate("customer/order", json));
		assertTrue(error.getMessage().contains("customer/order"));
		assertTrue(error.getMessage().contains("n1"));
		assertTrue(error.getMessage().contains("missing-node"));
	}

	@Test
	public void shouldRejectWorkflowWithoutStartToEndPath() {
		String json = "{\"nodes\":["
				+ "{\"id\":\"start\",\"type\":\"startNode\"},"
				+ "{\"id\":\"end\",\"type\":\"endNode\"}],"
				+ "\"edges\":[{\"id\":\"e1\",\"source\":\"end\",\"target\":\"start\"}]}";

		TinyflowException error = assertThrows(TinyflowException.class,
				() -> validator.validate("customer/disconnected", json));

		assertTrue(error.getMessage().contains("customer/disconnected"));
		assertTrue(error.getMessage().contains("startNode"));
		assertTrue(error.getMessage().contains("endNode"));
	}

	@Test
	public void shouldReturnParsedDefinitionUsingResourceId() {
		String json = "{\"nodes\":["
				+ "{\"id\":\"start\",\"type\":\"startNode\"},"
				+ "{\"id\":\"end\",\"type\":\"endNode\"}],"
				+ "\"edges\":[{\"id\":\"e1\",\"source\":\"start\",\"target\":\"end\"}]}";

		ChainDefinition definition = validator.validate("customer/order", json);

		assertEquals("customer/order", definition.getId());
		assertEquals(2, definition.getNodes().size());
		assertEquals(1, definition.getEdges().size());
	}

	@Test
	public void shouldDistinguishMissingAndAmbiguousRepositoryPaths() {
		InMemoryTinyflowWorkflowRepository repository = new InMemoryTinyflowWorkflowRepository();
		assertNull(repository.findByPath("customer/order"));

		repository.save(new TinyflowWorkflow("wf-1", "customer/order", "Order A", "{}"));
		repository.save(new TinyflowWorkflow("wf-2", "customer/order", "Order B", "{}"));

		TinyflowException error = assertThrows(TinyflowException.class,
				() -> repository.findByPath("customer/order"));
		assertTrue(error.getMessage().contains("customer/order"));
		assertTrue(error.getMessage().contains("wf-1"));
		assertTrue(error.getMessage().contains("wf-2"));
	}

	@Test
	public void shouldPreserveNativeWorkflowJsonVerbatim() {
		String json = "  {\n\t\"nodes\":[],\n\t\"edges\":[]\n}\n";
		TinyflowWorkflow workflow = new TinyflowWorkflow("wf-1", "customer/order", "Order", json);

		assertEquals(json, workflow.getJson());
	}
}
