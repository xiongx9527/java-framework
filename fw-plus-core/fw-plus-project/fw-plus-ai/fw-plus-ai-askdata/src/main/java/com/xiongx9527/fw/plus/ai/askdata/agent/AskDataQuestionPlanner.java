package com.xiongx9527.fw.plus.ai.askdata.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * 将用户问题转换为可顺序执行的问题计划。
 */
public interface AskDataQuestionPlanner {

	/**
	 * 将单个自然语言问题规划为顺序执行的问题列表。
	 *
	 * <p>返回计划中的每个问题必须保持可独立执行；规划器不持有项目、操作人或数据源上下文，调用方必须在
	 * 后续执行阶段为每个问题传递同一个已校验的隔离上下文。字符串输入不能为空白。实现可以识别复合问题，
	 * 但不得在无法可靠拆分时凭空补充问题；此时应保留为单个问题。</p>
	 *
	 * @param question 用户提交的单个问题，不能为空白
	 * @return 按执行顺序排列的问题计划，至少包含一个非空白问题
	 * @throws IllegalArgumentException 当 {@code question} 为空或仅包含空白字符时抛出
	 */
	AskDataQuestionPlan plan(String question);

	/**
	 * 将调用方显式提供的多个问题规划为顺序执行的问题列表。
	 *
	 * <p>实现必须保持输入顺序和问题边界，不得合并、重排或向列表插入推测性问题。规划阶段不负责放宽
	 * 项目、操作人、数据源等隔离条件；调用方须在执行时对每个问题使用同一已校验上下文。输入列表、
	 * 其中的元素及其内容均不能为空白，不存在以默认问题回退的规则。</p>
	 *
	 * @param questions 调用方已拆分的问题列表，不能为空且元素不能为空白
	 * @return 与输入顺序一致的问题计划
	 * @throws IllegalArgumentException 当 {@code questions} 为空、包含 {@code null} 或空白问题时抛出
	 */
	AskDataQuestionPlan plan(List<String> questions);

	/**
	 * 创建仅支持显式列表与中英文分号拆分的参考规划器。
	 *
	 * @return 不依赖模型服务的简单规划器
	 */
	static AskDataQuestionPlanner simple() {
		return new Simple();
	}

	/**
	 * 仅处理显式列表及中文、英文分号分隔的复合问题。
	 */
	final class Simple implements AskDataQuestionPlanner {

		@Override
		public AskDataQuestionPlan plan(String question) {
			String normalizedQuestion = AskDataQuestionPlan.requireNonBlank(question, "question");
			String[] parts = normalizedQuestion.split("[；;]");
			List<String> questions = new ArrayList<String>(parts.length);
			for (String part : parts) {
				if (part.trim().length() > 0) {
					questions.add(part);
				}
			}
			return new AskDataQuestionPlan(questions);
		}

		@Override
		public AskDataQuestionPlan plan(List<String> questions) {
			return new AskDataQuestionPlan(questions);
		}
	}
}
