package dev.tinyflow.langchain4j.provider;

import org.junit.Test;

import java.io.DataInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class Langchain4jLlmClasspathTest {

	private static final String CLASS_RESOURCE =
			"dev/tinyflow/langchain4j/provider/Langchain4jLlm.class";

	@Test
	public void shouldExposeExactlyOneCompatibilityClass() throws Exception {
		Enumeration<URL> resources = getClass().getClassLoader().getResources(CLASS_RESOURCE);
		List<URL> locations = new ArrayList<URL>();
		while (resources.hasMoreElements()) {
			locations.add(resources.nextElement());
		}

		assertEquals("Langchain4jLlm classpath locations: " + locations, 1, locations.size());
	}

	@Test
	public void shouldCompileCompatibilityClassForJava8() throws Exception {
		InputStream stream = getClass().getClassLoader().getResourceAsStream(CLASS_RESOURCE);
		DataInputStream input = new DataInputStream(stream);
		try {
			assertEquals(0xCAFEBABE, input.readInt());
			input.readUnsignedShort();
			assertEquals(52, input.readUnsignedShort());
		} finally {
			input.close();
		}
	}
}
