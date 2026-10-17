package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.Rule;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutput;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 一份业务契约：声明“某个业务长什么样、怎么校验它、失败最多重试几次”。
 *
 * <p>业务契约是「一个通用入口 + N 份业务契约」里的 N 份。新增一份契约的完整流程：</p>
 *
 * <pre>{@code
 * // 1. 写内层结果的 JSON Schema（顶层必须是对象；需要列表就包一层字段，如 {"items": [...]}）
 * String schema = "{"
 *         + "\"type\":\"object\","
 *         + "\"properties\":{"
 *         +   "\"orderId\":{\"type\":\"string\"},"
 *         +   "\"needMoreInfo\":{\"type\":\"boolean\"},"
 *         +   "\"question\":{\"type\":\"string\"}},"
 *         + "\"required\":[\"orderId\"]}";
 *
 * // 2. 建契约：必填/类型/枚举/范围/正则等约束可直接写在 schema 里（会自动派生为校验规则），
 * //    只有 schema 表达不了的业务语义才用 rules(...) 手写
 * BusinessContract order = BusinessContract.builder("order")
 *         .schema(schema)
 *         .rules(Rule.range("quantity", 1, 99))
 *         .structured(true)          // 本业务默认产出结构化结果；不声明则回落全局默认
 *         .maxAttempts(3)            // 重试上限；不声明则回落全局配置
 *         .systemHint("你是订单抽取助手")  // 结构化轮次的系统提示；不声明则用默认
 *         .version("1")              // 契约版本，会放进信封的 v
 *         .build();
 *
 * // 3. 注册（重名会立即失败）
 * registry.register(order);
 * }</pre>
 *
 * <p><b>追问语义</b>：业务追问（例如提示用户补充信息）**就是一次成功的业务结果**——
 * 契约把“追问”表达成结果里的一个普通字段（如上面的 {@code needMoreInfo} / {@code question}），
 * 该轮信封为 {@code ok=true}、追问信息在 {@code result} 内部、{@code text} 为空串。
 * 只有模型输出**缺少契约要求的字段**时才按校验失败处理（{@code ok=false}）。框架不靠关键词猜追问。</p>
 *
 * <p>{@link #structuredOutput()} 把 schema 与规则装配成校验器：先由
 * {@link Rule#fromSchema(String)} 派生，再叠加手写规则，因此“手写规则”与“派生规则”会同时生效。</p>
 */
public final class BusinessContract {

	/** 契约版本默认值（信封 {@code v} 的取值） */
	public static final String DEFAULT_VERSION = "1";

	private final String name;

	private final String jsonSchema;

	private final List<Rule> rules;

	private final Boolean structured;

	private final Integer maxAttempts;

	private final String systemHint;

	private final String version;

	/** 校验器：构造期一次性装配（含 schema 派生规则），避免每轮重复派生 */
	private final StructuredOutput structuredOutput;

	private BusinessContract(Builder builder) {
		if (builder.name == null || builder.name.trim().isEmpty()) {
			throw new IllegalArgumentException("业务名称不能为空");
		}
		if (builder.jsonSchema == null || builder.jsonSchema.trim().isEmpty()) {
			throw new IllegalArgumentException("业务 " + builder.name + " 缺少内层 schema（schema 不能为空）");
		}
		if (builder.maxAttempts != null && builder.maxAttempts < 1) {
			throw new IllegalArgumentException("业务 " + builder.name + " 的 max-attempts 必须 >= 1（含首次）");
		}
		this.name = builder.name.trim();
		this.jsonSchema = builder.jsonSchema.trim();
		this.rules = Collections.unmodifiableList(new ArrayList<Rule>(builder.rules));
		this.structured = builder.structured;
		this.maxAttempts = builder.maxAttempts;
		this.systemHint = builder.systemHint == null || builder.systemHint.trim().isEmpty()
				? null
				: builder.systemHint;
		this.version = builder.version == null || builder.version.trim().isEmpty()
				? DEFAULT_VERSION
				: builder.version.trim();
		// 提前派生规则：schema 非法在这里就失败（fail-fast），而不是等某轮请求才炸
		Rule[] derived = Rule.fromSchema(this.jsonSchema);
		this.structuredOutput = StructuredOutput.of(this.jsonSchema)
				.rules(derived)
				.rules(this.rules.toArray(new Rule[0]));
	}

	public static Builder builder(String name) {
		return new Builder(name);
	}

	/** 业务名称（信封 {@code business} 的取值） */
	public String getName() {
		return name;
	}

	/** 内层结果的 JSON Schema 文本 */
	public String getJsonSchema() {
		return jsonSchema;
	}

	/** 手写校验规则（可为空；schema 派生的规则由 {@link #structuredOutput()} 自动叠加） */
	public List<Rule> getRules() {
		return rules;
	}

	/** 本业务是否默认产出结构化结果；{@code null} = 未声明，回落全局默认 */
	public Boolean getStructured() {
		return structured;
	}

	/** 本业务的结构化重试上限；{@code null} = 未声明，回落全局配置 */
	public Integer getMaxAttempts() {
		return maxAttempts;
	}

	/** 结构化轮次的系统提示；{@code null} = 用框架默认 */
	public String getSystemHint() {
		return systemHint;
	}

	/** 契约版本（信封 {@code v} 的取值，默认 {@value #DEFAULT_VERSION}） */
	public String getVersion() {
		return version;
	}

	/** 装配好的校验器（schema 派生规则 + 手写规则） */
	public StructuredOutput structuredOutput() {
		return structuredOutput;
	}

	@Override
	public String toString() {
		return "BusinessContract{" + name + ", v=" + version + "}";
	}

	/** 契约构建器 */
	public static final class Builder {

		private final String name;

		private String jsonSchema;

		private final List<Rule> rules = new ArrayList<Rule>();

		private Boolean structured;

		private Integer maxAttempts;

		private String systemHint;

		private String version;

		private Builder(String name) {
			this.name = name;
		}

		/** 内层结果的 JSON Schema（必填） */
		public Builder schema(String jsonSchema) {
			this.jsonSchema = jsonSchema;
			return this;
		}

		/** 追加手写规则（schema 表达不了的业务语义），可多次调用累加 */
		public Builder rules(Rule... rules) {
			if (rules != null) {
				this.rules.addAll(Arrays.asList(rules));
			}
			return this;
		}

		/** 本业务是否默认产出结构化结果（不声明则回落全局默认） */
		public Builder structured(Boolean structured) {
			this.structured = structured;
			return this;
		}

		/** 结构化重试上限（含首次，必须 >= 1；不声明则回落全局配置） */
		public Builder maxAttempts(Integer maxAttempts) {
			this.maxAttempts = maxAttempts;
			return this;
		}

		/** 结构化轮次的系统提示 */
		public Builder systemHint(String systemHint) {
			this.systemHint = systemHint;
			return this;
		}

		/** 契约版本（默认 {@value BusinessContract#DEFAULT_VERSION}） */
		public Builder version(String version) {
			this.version = version;
			return this;
		}

		public BusinessContract build() {
			return new BusinessContract(this);
		}
	}
}
