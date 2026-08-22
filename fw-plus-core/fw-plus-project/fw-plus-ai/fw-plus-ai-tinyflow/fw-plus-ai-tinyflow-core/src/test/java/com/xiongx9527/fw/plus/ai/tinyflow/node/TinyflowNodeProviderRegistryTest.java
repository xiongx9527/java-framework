package com.xiongx9527.fw.plus.ai.tinyflow.node;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import dev.tinyflow.core.parser.DefaultNodeParsers;
import dev.tinyflow.core.parser.NodeParser;
import dev.tinyflow.core.parser.impl.StartNodeParser;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;

public class TinyflowNodeProviderRegistryTest {

	@Test
	public void shouldRejectSameTypeWithSamePriority() {
		assertThrows(TinyflowException.class,
				() -> new TinyflowNodeProviderRegistry(Arrays.asList(provider("custom", 0), provider("custom", 0))));
	}

	@Test
	public void shouldSelectUniqueHighestPriorityProvider() {
		TinyflowNodeProvider lowPriority = provider("custom", 1);
		TinyflowNodeProvider highPriority = provider("custom", 10);

		TinyflowNodeProviderRegistry registry = new TinyflowNodeProviderRegistry(
				Arrays.asList(lowPriority, highPriority));

		assertSame(highPriority, registry.getProvider("custom"));
		assertSame(highPriority.getParser(), registry.getParsers().get("custom"));
		assertEquals(Collections.singleton("custom"), registry.getParsers().keySet());
	}

	@Test
	public void shouldNotModifyTinyflowGlobalDefaultParserTable() {
		Set<String> originalTypes = new HashSet<String>(DefaultNodeParsers.getDefaultNodeParsers().keySet());

		new TinyflowNodeProviderRegistry(Collections.singletonList(provider("custom", 10)));

		assertEquals(originalTypes, DefaultNodeParsers.getDefaultNodeParsers().keySet());
		assertFalse(DefaultNodeParsers.getDefaultNodeParsers().containsKey("custom"));
	}

	private TinyflowNodeProvider provider(final String type, final int order) {
		return new TinyflowNodeProvider() {
			private final NodeParser<?> parser = new StartNodeParser();

			@Override
			public String getType() {
				return type;
			}

			@Override
			public NodeParser<?> getParser() {
				return parser;
			}

			@Override
			public TinyflowNodeDescriptor getDescriptor() {
				return null;
			}

			@Override
			public int getOrder() {
				return order;
			}
		};
	}
}
