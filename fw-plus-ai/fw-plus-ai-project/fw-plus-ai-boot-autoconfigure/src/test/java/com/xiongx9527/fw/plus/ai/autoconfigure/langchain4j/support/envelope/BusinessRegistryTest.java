package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.Rule;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.structured.StructuredOutput;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 业务契约与注册表契约测试：契约声明内容、重名 fail-fast、未注册回落兜底 `chat`、
 * schema 派生的校验规则真的被接上。
 */
class BusinessRegistryTest {

	private static final String ORDER_SCHEMA = "{"
			+ "\"type\":\"object\","
			+ "\"properties\":{\"orderId\":{\"type\":\"string\"},\"quantity\":{\"type\":\"integer\"}},"
			+ "\"required\":[\"orderId\"]}";

	private static final String LOOSE_SCHEMA = "{\"type\":\"object\"}";

	private static BusinessContract orderContract() {
		return BusinessContract.builder("order").schema(ORDER_SCHEMA).build();
	}

	private static StructuredOutput.Validation validate(BusinessContract contract, String json) {
		return contract.structuredOutput().validate(JSONObject.parseObject(json));
	}

	// -------------------------------------------------- 注册与解析

	@Test
	void registersAndResolvesContract() {
		BusinessRegistry registry = new BusinessRegistry();
		BusinessContract contract = orderContract();

		registry.register(contract);

		assertThat(registry.resolve("order")).isSameAs(contract);
		assertThat(registry.isRegistered("order")).isTrue();
		assertThat(registry.names()).containsExactly("order");
	}

	@Test
	void rejectsDuplicateNameAndPointsAtIt() {
		BusinessRegistry registry = new BusinessRegistry();
		registry.register(orderContract());

		assertThatThrownBy(() -> registry.register(orderContract()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("order");
		// 冲突时不静默覆盖：原来那份契约还在
		assertThat(registry.names()).containsExactly("order");
	}

	@Test
	void rejectsBlankName() {
		BusinessRegistry registry = new BusinessRegistry();

		assertThatThrownBy(() -> registry.register(BusinessContract.builder("  ").schema(ORDER_SCHEMA).build()))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void unregisteredNameFallsBackToChat() {
		BusinessRegistry registry = new BusinessRegistry();
		registry.register(orderContract());

		// 未注册不报错、返回 null，由调用方按兜底 chat 处理
		assertThat(registry.resolve("refund")).isNull();
		assertThat(registry.resolve(null)).isNull();
		assertThat(registry.resolve("")).isNull();
		assertThat(registry.isRegistered("refund")).isFalse();
		assertThat(BusinessRegistry.FALLBACK).isEqualTo("chat");
	}

	@Test
	void registeredNamesAreListedInRegistrationOrder() {
		BusinessRegistry registry = new BusinessRegistry();
		registry.register(orderContract());
		registry.register(BusinessContract.builder("refund").schema(ORDER_SCHEMA).build());

		assertThat(registry.names()).containsExactly("order", "refund");
	}

	// -------------------------------------------------- 契约本身

	@Test
	void contractRequiresSchema() {
		assertThatThrownBy(() -> BusinessContract.builder("order").build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("schema");
		assertThatThrownBy(() -> BusinessContract.builder("order").schema("   ").build())
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void contractFailsFastOnIllegalSchema() {
		// schema 顶层必须是对象：非法 schema 在建契约时就失败，不等到某轮请求才炸
		assertThatThrownBy(() -> BusinessContract.builder("order").schema("[]").build())
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> BusinessContract.builder("order").schema("不是 JSON").build())
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void contractDefaultsAreApplied() {
		BusinessContract contract = orderContract();

		assertThat(contract.getName()).isEqualTo("order");
		assertThat(contract.getVersion()).isEqualTo("1");
		// 未声明：结构化开关与重试上限回落到全局配置
		assertThat(contract.getStructured()).isNull();
		assertThat(contract.getMaxAttempts()).isNull();
		assertThat(contract.getSystemHint()).isNull();
	}

	@Test
	void contractDeclaredValuesAreKept() {
		BusinessContract contract = BusinessContract.builder("order")
				.schema(ORDER_SCHEMA)
				.structured(true)
				.maxAttempts(5)
				.systemHint("你是订单抽取助手")
				.version("2")
				.build();

		assertThat(contract.getStructured()).isTrue();
		assertThat(contract.getMaxAttempts()).isEqualTo(5);
		assertThat(contract.getSystemHint()).isEqualTo("你是订单抽取助手");
		assertThat(contract.getVersion()).isEqualTo("2");
	}

	@Test
	void contractRejectsNonPositiveMaxAttempts() {
		assertThatThrownBy(() -> BusinessContract.builder("order").schema(ORDER_SCHEMA).maxAttempts(0).build())
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("max-attempts");
	}

	// -------------------------------------------------- 校验器装配

	@Test
	void derivedRulesFromSchemaAreWiredIn() {
		// 零手写规则：必填约束由 schema 派生，缺字段必须判失败
		BusinessContract contract = orderContract();

		assertThat(validate(contract, "{\"orderId\":\"A1\",\"quantity\":2}").isValid()).isTrue();
		assertThat(validate(contract, "{\"quantity\":2}").isValid()).isFalse();
		assertThat(validate(contract, "{\"quantity\":2}").errors()).anyMatch(error -> error.contains("orderId"));
	}

	@Test
	void declaredRulesAreAddedOnTopOfDerivedOnes() {
		BusinessContract contract = BusinessContract.builder("order")
				.schema(ORDER_SCHEMA)
				.rules(Rule.range("quantity", 1, 99))
				.build();

		StructuredOutput.Validation validation = validate(contract, "{\"orderId\":\"A1\",\"quantity\":0}");
		assertThat(validation.isValid()).isFalse();
		assertThat(validation.errors()).anyMatch(error -> error.contains("quantity"));
	}

	@Test
	void businessWithZeroRulesIsAllowed() {
		BusinessContract contract = BusinessContract.builder("chatlike").schema(LOOSE_SCHEMA).build();

		assertThat(validate(contract, "{}").isValid()).isTrue();
	}
}
