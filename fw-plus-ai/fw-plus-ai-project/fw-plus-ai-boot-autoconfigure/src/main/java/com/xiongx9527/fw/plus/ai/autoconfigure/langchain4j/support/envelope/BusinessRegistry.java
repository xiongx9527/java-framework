package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务契约注册表：按名称注册与解析业务契约。
 *
 * <p>两条硬规则：</p>
 * <ul>
 *   <li><b>重名 fail-fast</b>：注册同名契约立即抛 {@link IllegalArgumentException} 并指出冲突的业务名称，
 *       不静默覆盖（覆盖会让“为什么这轮跑的是另一份 schema”变成玄学）。</li>
 *   <li><b>未注册回落兜底</b>：解析未注册的名称返回 {@code null}，由调用方按兜底业务
 *       {@value #FALLBACK}（纯闲聊）处理——通用入口因此在任何输入下都能给出结果，不会报错。</li>
 * </ul>
 *
 * <p>注册顺序即 {@link #names()} 的顺序（同时就是分类提示词里业务清单的顺序）。</p>
 */
public final class BusinessRegistry {

	/** 兜底业务名：本轮没有命中任何已注册契约时使用（纯闲聊轮次） */
	public static final String FALLBACK = "chat";

	private final Map<String, BusinessContract> contracts = new LinkedHashMap<String, BusinessContract>();

	/**
	 * 注册一份契约。
	 *
	 * @throws IllegalArgumentException 名称为空或与已注册契约重名
	 */
	public BusinessRegistry register(BusinessContract contract) {
		if (contract == null) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.BusinessRegistry.contractRequired"));
		}
		String name = contract.getName();
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException(
					I18nMessageResolver.resolveKey("fw.ai.BusinessRegistry.nameRequired"));
		}
		if (FALLBACK.equals(name)) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
					"fw.ai.BusinessRegistry.fallbackNotRegistrable", FALLBACK));
		}
		BusinessContract exists = contracts.get(name);
		if (exists != null) {
			throw new IllegalArgumentException(I18nMessageResolver.resolveKey(
					"fw.ai.BusinessRegistry.nameDuplicated", name, exists));
		}
		contracts.put(name, contract);
		return this;
	}

	/**
	 * 按名称解析契约。
	 *
	 * @return 已注册的契约；名称为空或未注册时返回 {@code null}（调用方按兜底 {@value #FALLBACK} 处理）
	 */
	public BusinessContract resolve(String name) {
		if (name == null || name.trim().isEmpty()) {
			return null;
		}
		return contracts.get(name.trim());
	}

	/** 是否已注册该名称 */
	public boolean isRegistered(String name) {
		return resolve(name) != null;
	}

	/** 已注册的业务名清单（注册顺序） */
	public List<String> names() {
		return Collections.unmodifiableList(new ArrayList<String>(contracts.keySet()));
	}

	/** 是否一份契约都没有注册（此时所有轮次都会落兜底 {@value #FALLBACK}） */
	public boolean isEmpty() {
		return contracts.isEmpty();
	}
}
