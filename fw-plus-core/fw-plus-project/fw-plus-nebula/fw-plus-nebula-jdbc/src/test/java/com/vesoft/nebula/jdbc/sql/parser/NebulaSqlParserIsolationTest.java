package com.vesoft.nebula.jdbc.sql.parser;

import com.vesoft.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class NebulaSqlParserIsolationTest {

	@Test
	public void parserDoesNotReferenceNebulaExecutionClasses() throws Exception {
		File parserRoot = new File(System.getProperty("user.dir"),
				"src/main/java/com/vesoft/nebula/jdbc/sql/parser");
		List<File> javaFiles = new ArrayList<File>();
		collectJavaFiles(parserRoot, javaFiles);

		for (File javaFile : javaFiles) {
			String source = new String(Files.readAllBytes(javaFile.toPath()), StandardCharsets.UTF_8);
			assertFalse(javaFile.getName(), source.contains("NebulaClientFacade"));
			assertFalse(javaFile.getName(), source.contains("NebulaConnection"));
			assertFalse(javaFile.getName(), source.contains("executeNgql"));
			assertFalse(javaFile.getName(), source.contains("DirectNebulaSqlExecutor"));
		}
	}

	@Test
	public void parserReturnsIrWithoutGeneratingNgql() throws Exception {
		NebulaSqlParseResult result = new DruidNebulaSqlParser().parse("SELECT id FROM person WHERE id = ?");

		assertEquals(StatementKind.SELECT, result.getKind());
		assertFalse(result.getDiagnostic().isRawFallback());
		assertFalse(result.getDiagnostic().getSqlSnippet().toUpperCase().contains("MATCH "));
	}

	private void collectJavaFiles(File dir, List<File> javaFiles) {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				collectJavaFiles(file, javaFiles);
			}
			else if (file.getName().endsWith(".java")) {
				javaFiles.add(file);
			}
		}
	}
}
