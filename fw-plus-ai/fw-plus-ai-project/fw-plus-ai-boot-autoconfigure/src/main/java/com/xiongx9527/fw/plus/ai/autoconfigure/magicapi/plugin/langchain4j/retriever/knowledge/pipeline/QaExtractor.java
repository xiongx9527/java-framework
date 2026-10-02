package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.pipeline;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import dev.langchain4j.model.chat.ChatLanguageModel;

import java.util.ArrayList;
import java.util.List;

/**
 * QA 分段抽取器（设计 §2.3）：对父段文本 prompt 抽取 {question, answer} 生成 QA 分段。
 *
 * <p>当前版本预留：kb_dataset 暂无 chat model key 字段，{@code segment_mode=qa} 的数据集
 * 在索引期给出可操作提示；待数据集增加 chat model 配置后接入本类。</p>
 */
public final class QaExtractor {

	private QaExtractor() {
	}

	private static final String PROMPT_TEMPLATE = "请从以下文本中抽取尽可能多的问答对，"
			+ "以 JSON 数组格式输出，每个元素形如 {\"question\":\"...\",\"answer\":\"...\"}，"
			+ "不要输出任何其他内容：\n\n%s";

	/** 对单个父段抽取 QA 对；模型输出不合规范时返回空列表。 */
	public static List<String[]> extract(ChatLanguageModel chatModel, String parentText) {
		List<String[]> pairs = new ArrayList<>();
		String response = chatModel.generate(String.format(PROMPT_TEMPLATE, parentText));
		if (response == null || response.isEmpty()) {
			return pairs;
		}
		String json = stripCodeFence(response);
		int start = json.indexOf('[');
		int end = json.lastIndexOf(']');
		if (start < 0 || end <= start) {
			return pairs;
		}
		try {
			JSONArray array = JSON.parseArray(json.substring(start, end + 1));
			for (int i = 0; i < array.size(); i++) {
				JSONObject item = array.getJSONObject(i);
				String question = item.getString("question");
				String answer = item.getString("answer");
				if (question != null && !question.isEmpty() && answer != null && !answer.isEmpty()) {
					pairs.add(new String[] { question, answer });
				}
			}
		} catch (Exception ignored) {
			// 模型输出不合规范时按抽取失败处理，返回已解析部分
		}
		return pairs;
	}

	private static String stripCodeFence(String response) {
		String trimmed = response.trim();
		if (trimmed.startsWith("```")) {
			int first = trimmed.indexOf('\n');
			int last = trimmed.lastIndexOf("```");
			if (first > 0 && last > first) {
				return trimmed.substring(first + 1, last);
			}
		}
		return trimmed;
	}
}
