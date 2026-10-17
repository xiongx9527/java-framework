package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutput;

import java.util.List;

/**
 * 分类驱动的路由：把「本轮消息 + 最近若干条历史用户消息」变成「本轮业务 + 该业务的契约」。
 *
 * <p>路由结果只有两种：命中已注册契约（{@link RoutedTurn#getContract()} 非 null）或回落兜底
 * {@value BusinessRegistry#FALLBACK}（{@code contract == null}，按纯闲聊轮次处理）。</p>
 *
 * <p><b>刻意不做的事</b>：不缓存分类结果、不做「会话 ↔ 业务」绑定、不提供显式指定业务的入参。
 * 每轮都重新判定——同一会话里先聊业务 A、再聊业务 B、又聊回 A，都能各自判定正确，无需任何重置操作。</p>
 *
 * <p>历史由调用方（会话记忆）提供并共用一份：同一个 {@code memoryId} 的所有轮次看到同一份历史，
 * 业务切换不隔离、不重建历史。本类只读历史，从不改写它。</p>
 */
public final class BusinessRouter {

	private final BusinessRegistry registry;

	private final BusinessClassifier classifier;

	public BusinessRouter(BusinessRegistry registry, Langchain4jAssistantProperties.Classifier config) {
		this.registry = registry == null ? new BusinessRegistry() : registry;
		this.classifier = new BusinessClassifier(this.registry, config);
	}

	/**
	 * 判定本轮业务并解析出对应契约。
	 *
	 * @param chat               发一次分类请求的方式（直连模型的 {@code generate}；分类不写会话记忆）
	 * @param recentUserMessages 该会话最近的历史用户消息（从旧到新；只读）
	 * @param message            本轮用户消息原文
	 * @return 本轮路由结果；判定失败或未注册时回落兜底 {@value BusinessRegistry#FALLBACK}
	 */
	public RoutedTurn route(StructuredOutput.ChatFunction chat, List<String> recentUserMessages, String message) {
		String business = classifier.classify(chat, recentUserMessages, message);
		// 兜底业务名不会出现在注册表里（注册时会拒绝），因此 resolve 返回 null 即“兜底轮次”
		BusinessContract contract = registry.resolve(business);
		return contract == null ? RoutedTurn.fallback(business) : RoutedTurn.of(contract);
	}

	/** 本轮路由结果 */
	public static final class RoutedTurn {

		private final String business;

		private final BusinessContract contract;

		private RoutedTurn(String business, BusinessContract contract) {
			this.business = business;
			this.contract = contract;
		}

		static RoutedTurn of(BusinessContract contract) {
			return new RoutedTurn(contract.getName(), contract);
		}

		static RoutedTurn fallback(String business) {
			return new RoutedTurn(business == null || business.trim().isEmpty() ? BusinessRegistry.FALLBACK : business,
					null);
		}

		/** 本轮业务名（信封 {@code business} 的取值） */
		public String getBusiness() {
			return business;
		}

		/** 本轮业务契约；{@code null} = 兜底轮次（纯闲聊，无内层 schema） */
		public BusinessContract getContract() {
			return contract;
		}

		/** 是否兜底轮次 */
		public boolean isFallback() {
			return contract == null;
		}

		@Override
		public String toString() {
			return "RoutedTurn{" + business + (contract == null ? ", fallback" : ", v=" + contract.getVersion()) + "}";
		}
	}
}
