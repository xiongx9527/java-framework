package dev.langchain4j.agentic;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.lang.reflect.Modifier;

/**
 * Source-contract test for Task 2a1.
 *
 * <p>This test deliberately has no module or third-party dependency and validates the 15
 * migrated sources directly, preserving a complete and repeatable checklist.</p>
 */
public final class Task2a1SourceContractTest {

    private static final String SOURCE_ROOT = "src/main/java";

    private static final Map<String, String[]> COMPONENTS = new LinkedHashMap<String, String[]>();

    static {
        COMPONENTS.put("dev/langchain4j/agentic/observability/BeforeAgentToolExecution.java",
                new String[] {"agentInstance", "toolExecution"});
        COMPONENTS.put("dev/langchain4j/agentic/agent/ErrorContext.java",
                new String[] {"agentName", "agenticScope", "exception"});
        COMPONENTS.put("dev/langchain4j/agentic/scope/AgentInvocation.java",
                new String[] {"agentType", "agentName", "agentId", "input", "output"});
        COMPONENTS.put("dev/langchain4j/agentic/planner/InitPlanningContext.java",
                new String[] {"agenticScope", "plannerAgent", "subagents"});
        COMPONENTS.put("dev/langchain4j/agentic/observability/AgentRequest.java",
                new String[] {"agenticScope", "agent", "inputs"});
        COMPONENTS.put("dev/langchain4j/agentic/observability/AgentInvocationError.java",
                new String[] {"agenticScope", "agent", "inputs", "error"});
        COMPONENTS.put("dev/langchain4j/agentic/observability/AgentResponse.java",
                new String[] {"agenticScope", "agent", "inputs", "output", "chatRequest", "chatResponse"});
        COMPONENTS.put("dev/langchain4j/agentic/planner/AgentArgument.java",
                new String[] {"type", "name", "defaultValue", "isOptional"});
        COMPONENTS.put("dev/langchain4j/agentic/agent/ErrorRecoveryResult.java",
                new String[] {"type", "result"});
        COMPONENTS.put("dev/langchain4j/agentic/internal/AgentInvocationArguments.java",
                new String[] {"namedArgs", "positionalArgs"});
        COMPONENTS.put("dev/langchain4j/agentic/scope/AgenticScopeKey.java",
                new String[] {"agentId", "memoryId"});
        COMPONENTS.put("dev/langchain4j/agentic/observability/AfterAgentToolExecution.java",
                new String[] {"agentInstance", "toolExecution"});
        COMPONENTS.put("dev/langchain4j/agentic/workflow/ConditionalAgent.java",
                new String[] {"condition", "predicate", "agentInstances"});
        COMPONENTS.put("dev/langchain4j/agentic/workflow/impl/ConditionalPlanner.java",
                new String[] {"conditionalSubagents"});
        COMPONENTS.put("dev/langchain4j/agentic/planner/PlanningContext.java",
                new String[] {"agenticScope", "previousAgentInvocation"});
    }

    private Task2a1SourceContractTest() {
    }

    public static void main(String[] args) throws Exception {
        Path moduleRoot = args.length == 0 ? Paths.get(".") : Paths.get(args[0]);
        verifyCompleteness(moduleRoot);
        verifyRecordEquivalentShape(moduleRoot);
        System.out.println("Task 2a1 source-contract checks passed: " + COMPONENTS.size() + " types");
    }

    private static void verifyCompleteness(Path moduleRoot) throws IOException {
        assertEquals(15, COMPONENTS.size(), "Task 2a1 checklist must contain exactly 15 types");
        for (Map.Entry<String, String[]> entry : COMPONENTS.entrySet()) {
            String source = read(moduleRoot.resolve(SOURCE_ROOT).resolve(entry.getKey()));
            String simpleName = simpleName(entry.getKey());
            assertTrue(!Pattern.compile("\\brecord\\b").matcher(source).find(),
                    simpleName + " still contains a record declaration");
            assertTrue(source.contains("public final class " + simpleName),
                    simpleName + " is not a public final class");
            for (String component : entry.getValue()) {
                assertTrue(Pattern.compile("private\\s+final\\s+[^;\\n]+\\s+" + component + "\\s*;")
                                .matcher(source).find(),
                        simpleName + " missing private final component field " + component);
                assertTrue(Pattern.compile("public\\s+[^\\n{]+\\s+" + component + "\\s*\\(\\s*\\)")
                                .matcher(source).find(),
                        simpleName + " missing component accessor " + component + "()");
            }
            assertTrue(source.contains("boolean equals(Object o)"), simpleName + " missing equals");
            assertTrue(source.contains("int hashCode()"), simpleName + " missing hashCode");
            assertTrue(source.contains("String toString()"), simpleName + " missing toString");
            assertTrue(source.contains("Objects.equals") || source.contains("== null ? 0 :"),
                    simpleName + " does not visibly preserve null-safe value comparison/hash semantics");
            if (!"AgentInvocation".equals(simpleName)) {
                assertTrue(source.contains(simpleName + "["), simpleName + " toString does not use record format");
            }
            assertTrue(hasCanonicalConstructor(source, simpleName, entry.getValue()),
                    simpleName + " canonical constructor signature/order is missing");
        }
    }

    private static void verifyRecordEquivalentShape(Path moduleRoot) throws IOException {
        for (Map.Entry<String, String[]> entry : COMPONENTS.entrySet()) {
            String source = read(moduleRoot.resolve(SOURCE_ROOT).resolve(entry.getKey()));
            String simpleName = simpleName(entry.getKey());
            assertTrue(source.contains("public " + simpleName + "(")
                            || source.contains("public " + simpleName + "<"),
                    simpleName + " has no construction path");
            assertTrue(hasCanonicalConstructor(source, simpleName, entry.getValue()),
                    simpleName + " component order changed in canonical constructor");
            assertTrue(source.contains("Objects.equals") || source.contains("== null"),
                    simpleName + " lacks null-component equality handling");
            assertTrue(source.contains("31 * hash") || source.contains("31 * result")
                            || source.contains("conditionalSubagents.hashCode()"),
                    simpleName + " lacks record-style hash accumulation");
        }
    }


    private static boolean hasCanonicalConstructor(String source, String simpleName, String[] components) {
        int constructor = source.indexOf("public " + simpleName + "(");
        if (constructor < 0) {
            constructor = source.indexOf("public " + simpleName + "<");
            if (constructor < 0) {
                return false;
            }
            constructor = source.indexOf("(", constructor);
        }
        int close = source.indexOf(")", constructor);
        if (close < 0) {
            return false;
        }
        String signature = source.substring(constructor, close);
        int previous = -1;
        for (String component : components) {
            java.util.regex.Matcher matcher = Pattern.compile("\\b" + component + "\\s*(?:,|$)")
                    .matcher(signature);
            if (!matcher.find() || matcher.start() <= previous) {
                return false;
            }
            previous = matcher.start();
        }
        return true;
    }

    private static String read(Path path) throws IOException {
        assertTrue(Files.exists(path), "Missing source file " + path);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    private static String simpleName(String path) {
        String file = path.substring(path.lastIndexOf('/') + 1);
        return file.substring(0, file.length() - ".java".length());
    }


    private static void assertEquals(int expected, int actual, String message) {
        assertTrue(expected == actual, message + ": expected " + expected + ", got " + actual);
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
