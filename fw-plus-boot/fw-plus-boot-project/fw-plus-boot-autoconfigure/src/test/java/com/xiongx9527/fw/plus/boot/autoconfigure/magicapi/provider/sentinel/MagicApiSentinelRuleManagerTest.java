package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.support.GenericApplicationContext;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.Option;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MagicApiSentinelRuleManagerTest {

	private GenericApplicationContext context;
	private final MagicApiSentinelResourceResolver resourceResolver = new MagicApiSentinelResourceResolver();
	private final MagicApiSentinelRuleManager ruleManager = new MagicApiSentinelRuleManager();

	@BeforeEach
	void setUp() {
		FlowRuleManager.loadRules(Collections.emptyList());
		ParamFlowRuleManager.loadRules(Collections.emptyList());
		context = new GenericApplicationContext();
		context.refresh();
		new SpringUtils().setApplicationContext(context);
	}

	@AfterEach
	void tearDown() {
		FlowRuleManager.loadRules(Collections.emptyList());
		ParamFlowRuleManager.loadRules(Collections.emptyList());
		if (context != null) {
			context.close();
		}
	}

	@Test
	void resourceNamePrefersApiId() {
		ApiInfo info = api("api-1", "/orders/create", "POST");

		assertThat(resourceResolver.resolve(info)).isEqualTo("magicapi:api-1");
	}

	@Test
	void resourceNameFallsBackToMethodAndPath() {
		ApiInfo info = api(null, "orders/create", "POST");

		assertThat(resourceResolver.resolve(info)).isEqualTo("magicapi:POST:/orders/create");
	}

	@Test
	void optionsParserMapsGradesCountsAndDuration() {
		ApiInfo info = api("api-2", "/hot", "GET",
				option(Options.HOT_DATA_ENABLE, "true"),
				option(Options.HOT_DATA_GRADE, "THREAD"),
				option(Options.HOT_DATA_COUNT, "5"),
				option(Options.HOT_DATA_DURATION, "30"),
				option(Options.FALLBACK_ENABLE, "true"),
				option(Options.FALLBACK_COUNT, "7"));

		MagicApiSentinelOptions options = MagicApiSentinelOptions.from(info);

		assertThat(options.isHotDataEnabled()).isTrue();
		assertThat(options.getHotDataGrade()).isEqualTo(RuleConstant.FLOW_GRADE_THREAD);
		assertThat(options.getHotDataCount()).isEqualTo(5D);
		assertThat(options.getHotDataDuration()).isEqualTo(30L);
		assertThat(options.isFallbackEnabled()).isTrue();
		assertThat(options.getFallbackCount()).isEqualTo(7D);
	}

	@Test
	void externalConfigOverridesInterfaceOptions() {
		context.close();
		context = new GenericApplicationContext();
		TestPropertyValues.of(
				"magic.api./hot.options.hot_data_count=9",
				"magic.api./hot.options.fallback_enable=false").applyTo(context);
		context.refresh();
		new SpringUtils().setApplicationContext(context);
		ApiInfo info = api("api-3", "/hot", "GET",
				option(Options.HOT_DATA_ENABLE, "true"),
				option(Options.HOT_DATA_COUNT, "3"),
				option(Options.FALLBACK_ENABLE, "true"),
				option(Options.FALLBACK_COUNT, "4"));

		MagicApiSentinelOptions options = MagicApiSentinelOptions.from(info);

		assertThat(options.getHotDataCount()).isEqualTo(9D);
		assertThat(options.isFallbackEnabled()).isFalse();
	}

	@Test
	void nonPositiveCountsDoNotCreateRules() {
		MagicApiSentinelOptions options = new MagicApiSentinelOptions(true, RuleConstant.FLOW_GRADE_QPS, 0D, 60L,
				true, 0D);

		ruleManager.ensureRules("magicapi:empty", options);

		assertThat(FlowRuleManager.getRules()).isEmpty();
		assertThat(ParamFlowRuleManager.getRules()).isEmpty();
	}

	@Test
	void createsHotDataAndFallbackRules() {
		MagicApiSentinelOptions options = new MagicApiSentinelOptions(true, RuleConstant.FLOW_GRADE_QPS, 5D, 60L,
				true, 3D);

		ruleManager.ensureRules("magicapi:orders", options);

		List<FlowRule> flowRules = FlowRuleManager.getRules();
		assertThat(flowRules).hasSize(1);
		assertThat(flowRules.get(0).getResource()).isEqualTo("magicapi:orders");
		assertThat(flowRules.get(0).getCount()).isEqualTo(3D);
		assertThat(flowRules.get(0).getGrade()).isEqualTo(RuleConstant.FLOW_GRADE_QPS);

		List<ParamFlowRule> paramRules = ParamFlowRuleManager.getRules();
		assertThat(paramRules).hasSize(1);
		assertThat(paramRules.get(0).getResource()).isEqualTo("magicapi:orders");
		assertThat(paramRules.get(0).getCount()).isEqualTo(5D);
		assertThat(paramRules.get(0).getGrade()).isEqualTo(RuleConstant.FLOW_GRADE_QPS);
		assertThat(paramRules.get(0).getDurationInSec()).isEqualTo(60L);
		assertThat(paramRules.get(0).getParamIdx()).isEqualTo(0);
	}

	@Test
	void updatingOneResourceKeepsOtherMagicApiAndExternalRules() {
		FlowRule externalFlow = new FlowRule("external:resource").setCount(99D);
		FlowRuleManager.loadRules(Collections.singletonList(externalFlow));
		ParamFlowRule externalParam = new ParamFlowRule("external:param").setCount(88D).setParamIdx(0);
		ParamFlowRuleManager.loadRules(Collections.singletonList(externalParam));
		MagicApiSentinelOptions first = new MagicApiSentinelOptions(true, RuleConstant.FLOW_GRADE_QPS, 5D, 60L,
				true, 3D);
		MagicApiSentinelOptions second = new MagicApiSentinelOptions(true, RuleConstant.FLOW_GRADE_THREAD, 6D, 30L,
				true, 4D);

		ruleManager.ensureRules("magicapi:a", first);
		ruleManager.ensureRules("magicapi:b", second);
		ruleManager.ensureRules("magicapi:a", new MagicApiSentinelOptions(true, RuleConstant.FLOW_GRADE_QPS, 10D, 120L,
				true, 8D));

		assertThat(FlowRuleManager.getRules())
				.extracting(FlowRule::getResource)
				.containsExactlyInAnyOrder("external:resource", "magicapi:a", "magicapi:b");
		assertThat(FlowRuleManager.getRules())
				.filteredOn(rule -> "magicapi:a".equals(rule.getResource()))
				.singleElement()
				.extracting(FlowRule::getCount)
				.isEqualTo(8D);
		assertThat(ParamFlowRuleManager.getRules())
				.extracting(ParamFlowRule::getResource)
				.containsExactlyInAnyOrder("external:param", "magicapi:a", "magicapi:b");
		assertThat(ParamFlowRuleManager.getRules())
				.filteredOn(rule -> "magicapi:a".equals(rule.getResource()))
				.singleElement()
				.extracting(ParamFlowRule::getCount)
				.isEqualTo(10D);
	}

	private ApiInfo api(String id, String path, String method, Option... options) {
		ApiInfo info = new ApiInfo();
		info.setId(id);
		info.setPath(path);
		info.setMethod(method);
		info.setOption(options == null ? Collections.emptyList() : Arrays.asList(options));
		return info;
	}

	private Option option(Options option, String value) {
		return new Option(option.getValue(), value);
	}
}
