package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MagicApiSentinelRuleManager {

	private final Map<String, FlowRule> flowRules = new LinkedHashMap<>();
	private final Map<String, ParamFlowRule> paramFlowRules = new LinkedHashMap<>();

	public synchronized void ensureRules(String resource, MagicApiSentinelOptions options) {
		if (options == null) {
			flowRules.remove(resource);
			paramFlowRules.remove(resource);
			loadFlowRules();
			loadParamFlowRules();
			return;
		}
		if (options.isFallbackEffective()) {
			flowRules.put(resource, createFlowRule(resource, options));
		} else {
			flowRules.remove(resource);
		}
		if (options.isHotDataEffective()) {
			paramFlowRules.put(resource, createParamFlowRule(resource, options));
		} else {
			paramFlowRules.remove(resource);
		}
		loadFlowRules();
		loadParamFlowRules();
	}

	private FlowRule createFlowRule(String resource, MagicApiSentinelOptions options) {
		return new FlowRule(resource)
				.setGrade(RuleConstant.FLOW_GRADE_QPS)
				.setCount(options.getFallbackCount());
	}

	private ParamFlowRule createParamFlowRule(String resource, MagicApiSentinelOptions options) {
		return new ParamFlowRule(resource)
				.setGrade(options.getHotDataGrade())
				.setCount(options.getHotDataCount())
				.setDurationInSec(options.getHotDataDuration())
				.setParamIdx(0);
	}

	private void loadFlowRules() {
		List<FlowRule> merged = new ArrayList<>();
		for (FlowRule rule : FlowRuleManager.getRules()) {
			if (!flowRules.containsKey(rule.getResource())) {
				merged.add(rule);
			}
		}
		merged.addAll(flowRules.values());
		FlowRuleManager.loadRules(merged);
	}

	private void loadParamFlowRules() {
		List<ParamFlowRule> merged = new ArrayList<>();
		for (ParamFlowRule rule : ParamFlowRuleManager.getRules()) {
			if (!paramFlowRules.containsKey(rule.getResource())) {
				merged.add(rule);
			}
		}
		merged.addAll(paramFlowRules.values());
		ParamFlowRuleManager.loadRules(merged);
	}
}
