package com.xiongx9527.fw.plus.ai.askdata.agent;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 默认 AskData Flow 可被业务 Agent 替换的稳定步骤 key。
 */
public final class AskDataAgentStepKey {

	public static final String SELECT_TABLES = "select-tables";
	public static final String GENERATE_SQL = "generate-sql";
	public static final String VALIDATE_SQL = "validate-sql";
	public static final String EXECUTE_SQL = "execute-sql";
	public static final String INTERPRET_RESULT = "interpret-result";

	private static final Set<String> SUPPORTED = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(
			SELECT_TABLES, GENERATE_SQL, VALIDATE_SQL, EXECUTE_SQL, INTERPRET_RESULT)));

	private AskDataAgentStepKey() {
	}

	public static boolean isSupported(String stepKey) {
		return stepKey != null && SUPPORTED.contains(stepKey.trim());
	}

	public static Set<String> supported() {
		return SUPPORTED;
	}
}
