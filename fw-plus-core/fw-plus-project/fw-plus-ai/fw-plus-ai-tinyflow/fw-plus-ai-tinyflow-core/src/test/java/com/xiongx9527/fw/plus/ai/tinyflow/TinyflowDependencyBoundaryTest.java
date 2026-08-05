package com.xiongx9527.fw.plus.ai.tinyflow;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TinyflowDependencyBoundaryTest {
	@Test
	public void shouldLoadJava8TinyflowCoreWithoutOfficialLangchain4jAdapter() {
		assertNotNull(dev.tinyflow.core.parser.ChainParser.class);
		assertNull(TinyflowDependencyBoundaryTest.class.getClassLoader()
				.getResource("META-INF/maven/dev.tinyflow/tinyflow-support-langchain4j/pom.properties"));
	}
}
