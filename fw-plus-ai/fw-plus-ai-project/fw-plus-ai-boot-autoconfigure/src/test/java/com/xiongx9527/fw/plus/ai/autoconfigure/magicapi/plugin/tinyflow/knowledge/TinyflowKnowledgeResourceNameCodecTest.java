package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class TinyflowKnowledgeResourceNameCodecTest {

	@Test
	public void shouldEncodeResourceIdentityReversiblyWithoutMagicFilenameCharacters() {
		List<String> identity = Arrays.asList("chunk", "tenant/a", "kb with space", "同一个/chunk:id");

		String encoded = TinyflowKnowledgeResourceNameCodec.encode(identity);

		assertTrue(encoded.matches("[A-Za-z0-9_.-]+"));
		assertEquals(identity, TinyflowKnowledgeResourceNameCodec.decode(encoded));
	}

	@Test
	public void shouldKeepScopedResourceNamesCollisionFree() {
		String first = TinyflowKnowledgeResourceNameCodec.encode(
				Arrays.asList("chunk", "tenant-a", "kb-1", "chunk-shared"));
		String second = TinyflowKnowledgeResourceNameCodec.encode(
				Arrays.asList("chunk", "tenant-a", "kb-2", "chunk-shared"));

		assertNotEquals(first, second);
		assertFalse(first.startsWith("."));
		assertFalse(second.startsWith("."));
	}
}
