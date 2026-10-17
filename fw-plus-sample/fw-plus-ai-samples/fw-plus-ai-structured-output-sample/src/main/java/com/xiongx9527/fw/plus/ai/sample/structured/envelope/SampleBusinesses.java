package com.xiongx9527.fw.plus.ai.sample.structured.envelope;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.BusinessContract;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.BusinessRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.Rule;

import java.util.Arrays;

/**
 * 信封助手的「N 份业务契约」：这里假两份业务，证明新增业务只需要多写一份契约。
 *
 * <p>两份业务的差异全部落在内层 {@code result} 上（外层信封永远是同样 5 个键）：</p>
 *
 * <ul>
 *   <li>{@link #order()}：订单查询，结果里允许模型<b>反问</b>（{@code needMoreInfo} / {@code question}），
 *       契约版本 {@code 1}；</li>
 *   <li>{@link #refund()}：退款申请，结果字段不同、反问补齐后要 {@code reason}，契约版本 {@code 2}。</li>
 * </ul>
 *
 * <p><b>反问不是特殊事件</b>：业务追问就是一次<b>成功</b>的业务结果（{@code ok=true}，追问信息在
 * {@code result} 内部）；只有模型输出不满足契约（例如声明 {@code needMoreInfo=false} 却没给订单号）
 * 才算失败。</p>
 *
 * <p>校验两道：JSON Schema 管结构，{@link Rule} lambda 补业务语义（schema 表达不了的）。</p>
 */
public final class SampleBusinesses {

	/** 业务名：订单查询（这个名字同时是信封 {@code business} 的取值与分类枚举的取值） */
	public static final String ORDER = "order";

	/** 业务名：退款申请 */
	public static final String REFUND = "refund";

	/** 订单查询的内层结果结构：只有 {@code needMoreInfo} 是必填，其余按需出现 */
	private static final String ORDER_SCHEMA = "{"
			+ "\"type\":\"object\","
			+ "\"properties\":{"
			+ "\"needMoreInfo\":{\"type\":\"boolean\"},"
			+ "\"question\":{\"type\":\"string\"},"
			+ "\"orderId\":{\"type\":\"string\"},"
			+ "\"status\":{\"type\":\"string\"}},"
			+ "\"required\":[\"needMoreInfo\"]}";

	/** 退款申请的内层结果结构：字段与订单完全不同，但外层信封形态一致 */
	private static final String REFUND_SCHEMA = "{"
			+ "\"type\":\"object\","
			+ "\"properties\":{"
			+ "\"needMoreInfo\":{\"type\":\"boolean\"},"
			+ "\"question\":{\"type\":\"string\"},"
			+ "\"orderId\":{\"type\":\"string\"},"
			+ "\"reason\":{\"type\":\"string\"}},"
			+ "\"required\":[\"needMoreInfo\"]}";

	private SampleBusinesses() {
	}

	/**
	 * 订单查询契约：结构化、最多尝试 2 次（失败会把校验错误回喂重试一次）、契约版本 1。
	 *
	 * <p>schema 表达的约束（必填、类型）由框架自动从 schema 派生；这里额外写一条业务语义：
	 * 声明「不用补充信息」就必须给出订单号。</p>
	 */
	public static BusinessContract order() {
		return BusinessContract.builder(ORDER)
				.schema(ORDER_SCHEMA)
				.rules(
						// 业务语义：不需要补充信息 = 本轮就该给出订单号
						value -> Boolean.FALSE.equals(value.getBoolean("needMoreInfo"))
								&& isBlank(value.getString("orderId"))
										? "needMoreInfo=false 时必须给出 orderId"
										: null,
						// 状态取值只用 schema 表达不了时才写 lambda（这里允许字段缺省）
						value -> value.getString("status") != null
								&& !Arrays.asList("paid", "unpaid").contains(value.getString("status"))
										? "status 只能是 paid 或 unpaid"
										: null)
				.structured(true)
				.maxAttempts(2)
				.systemHint("你是订单查询助手，只输出 JSON。")
				.version("1")
				.build();
	}

	/**
	 * 退款申请契约：另一份业务，另一套结果字段与另一条业务语义
	 * （要退款就得说清楚是哪个订单、什么理由），契约版本 2。
	 */
	public static BusinessContract refund() {
		return BusinessContract.builder(REFUND)
				.schema(REFUND_SCHEMA)
				.rules(value -> Boolean.FALSE.equals(value.getBoolean("needMoreInfo"))
						&& (isBlank(value.getString("orderId")) || isBlank(value.getString("reason")))
								? "needMoreInfo=false 时必须同时给出 orderId 与 reason"
								: null)
				.structured(true)
				.maxAttempts(2)
				.systemHint("你是退款申请助手，只输出 JSON。")
				.version("2")
				.build();
	}

	/** 把两份契约登记成注册表（业务名重名会立即失败，避免两个契约抢同一个名字） */
	public static BusinessRegistry registry() {
		BusinessRegistry registry = new BusinessRegistry();
		registry.register(order());
		registry.register(refund());
		return registry;
	}

	private static boolean isBlank(String text) {
		return text == null || text.trim().isEmpty();
	}
}
