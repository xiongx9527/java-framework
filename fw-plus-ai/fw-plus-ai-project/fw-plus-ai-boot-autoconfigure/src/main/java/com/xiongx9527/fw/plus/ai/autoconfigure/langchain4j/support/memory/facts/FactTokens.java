package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 本地分词与评分工具（keyword / rerank 策略共用，包内可见）：
 * ASCII 词（小写、&gt;= 2 字符）+ CJK 连续片段二元组；评分 = 查询 token 覆盖率。
 */
final class FactTokens {

	/** ASCII 词：字母 / 数字 / 下划线 */
	private static final Pattern ASCII_WORD = Pattern.compile("[a-z0-9_]+");

	/** CJK 连续片段 */
	private static final Pattern CJK_RUN = Pattern.compile("[\\u4e00-\\u9fff]+");

	private FactTokens() {
	}

	/** 分词：ASCII 词（小写、&gt;= 2 字符）+ CJK 二元组；保序去重。 */
	static Set<String> tokenize(String text) {
		Set<String> tokens = new LinkedHashSet<String>();
		if (text == null || text.trim().isEmpty()) {
			return tokens;
		}
		Matcher ascii = ASCII_WORD.matcher(text.toLowerCase(Locale.ROOT));
		while (ascii.find()) {
			String word = ascii.group();
			if (word.length() >= 2) {
				tokens.add(word);
			}
		}
		Matcher cjk = CJK_RUN.matcher(text);
		while (cjk.find()) {
			String run = cjk.group();
			for (int i = 0; i + 1 < run.length(); i++) {
				tokens.add(run.substring(i, i + 2));
			}
		}
		return tokens;
	}

	/** 查询 token 覆盖率 = |查询 ∩ 事实| / |查询|；查询无 token 时为 0。 */
	static double coverage(Set<String> queryTokens, Set<String> factTokens) {
		if (queryTokens == null || queryTokens.isEmpty() || factTokens == null || factTokens.isEmpty()) {
			return 0.0d;
		}
		int hit = 0;
		for (String token : queryTokens) {
			if (factTokens.contains(token)) {
				hit++;
			}
		}
		return (double) hit / queryTokens.size();
	}
}
