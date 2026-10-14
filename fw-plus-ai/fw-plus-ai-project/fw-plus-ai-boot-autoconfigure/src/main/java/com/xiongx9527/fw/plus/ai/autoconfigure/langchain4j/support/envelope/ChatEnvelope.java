package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.annotation.JSONField;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 统一聊天信封：恰好 5 个键 {@code ok} / {@code business} / {@code v} / {@code result} / {@code text}。
 *
 * <p>成功与失败**同形**：任何路径都输出同一组键，失败不会缺键。取值语义：</p>
 *
 * <ul>
 *   <li>{@code ok}：本轮是否成功（boolean）。</li>
 *   <li>{@code business}：本轮命中的业务名（由分类决定；兜底闲聊轮次为 {@code chat}）。</li>
 *   <li>{@code v}：业务契约版本（字符串，默认 {@value #DEFAULT_VERSION}）；只表达契约版本，不表达框架版本。</li>
 *   <li>{@code result}：业务结构化结果；{@code ok=false} 时恒为 {@code null}（键仍然存在）。</li>
 *   <li>{@code text}：“无结构可用时的可展示文本”——闲聊轮次为模型原文，失败轮次为兜底话术，
 *       结构化成功轮次为空串（人话由业务放进 {@code result}）。</li>
 * </ul>
 *
 * <p>外层**不得**出现任何业务语义字段（追问标记、缺失字段列表、意图、订单号等都只能出现在 {@code result} 内部）。</p>
 *
 * <p>本类不可变：所有 getter 与 {@link #toJsonObject()} 都返回**顶层副本**（内层对象共享，
 * 调用方不要改写内层结构），外部对副本的顶层改动不会回写内部状态。</p>
 *
 * <p><b>序列化提示</b>（{@code result} 为 {@code null} 时键必须保留，否则破坏“成功失败同形”）：</p>
 * <ul>
 *   <li>首选用 {@link #toJsonObject()} / {@link #toJsonString()}：两者恒好 5 键，与序列化器配置无关。</li>
 *   <li>直接序列化本对象也同形：{@code getVersion()} 已标注 {@code @JSONField(name="v")} 与
 *       {@code @JsonProperty("v")}，fastjson2 与 Jackson 都输出 {@code v} 而非 {@code version}。</li>
 *   <li>框架默认会丢 {@code null} 值：本类已在 {@code getResult()} 上标注
 *       {@code @JSONField(serializeFeatures = WriteNulls)}，fastjson2 直接序列化对象也保留 {@code result} 键。
 *       Jackson 默认保留，但不要开启 {@code spring.jackson.default-property-inclusion=non_null}。</li>
 * </ul>
 */
public final class ChatEnvelope {

	/** 键名：本轮是否成功 */
	public static final String KEY_OK = "ok";

	/** 键名：本轮命中的业务名 */
	public static final String KEY_BUSINESS = "business";

	/** 键名：业务契约版本 */
	public static final String KEY_VERSION = "v";

	/** 键名：业务结构化结果（失败时为 {@code null}，但键必须存在） */
	public static final String KEY_RESULT = "result";

	/** 键名：无结构可用时的可展示文本 */
	public static final String KEY_TEXT = "text";

	/** 业务契约版本默认值 */
	public static final String DEFAULT_VERSION = "1";

	/** 失败轮次的兜底话术：不给调用方看异常原文，也不暴露校验错误细节 */
	public static final String DEFAULT_FAILURE_TEXT = "抱歉，这次我没能完成处理，请换一种说法再试一次。";

	private static final List<String> KEYS = Collections.unmodifiableList(Arrays.asList(
			KEY_OK, KEY_BUSINESS, KEY_VERSION, KEY_RESULT, KEY_TEXT));

	private final boolean ok;

	private final String business;

	private final String version;

	private final JSONObject result;

	private final String text;

	private ChatEnvelope(boolean ok, String business, String version, JSONObject result, String text) {
		if (!ok && result != null) {
			// 不变量：ok=false ⇒ result=null，构造入口封死，保证任何路径都同形
			throw new IllegalArgumentException("ok=false 时 result 必须为 null");
		}
		this.ok = ok;
		this.business = business == null ? "" : business;
		this.version = version == null || version.trim().isEmpty() ? DEFAULT_VERSION : version.trim();
		this.result = result;
		this.text = text == null ? "" : text;
	}

	/** 结构化成功：{@code ok=true}、{@code result} 为业务对象、{@code text} 为空串 */
	public static ChatEnvelope structured(String business, JSONObject result, String version) {
		if (result == null) {
			throw new IllegalArgumentException("结构化成功必须带 result");
		}
		return new ChatEnvelope(true, business, version, result, "");
	}

	/** 结构化成功（契约版本取默认值） */
	public static ChatEnvelope structured(String business, JSONObject result) {
		return structured(business, result, DEFAULT_VERSION);
	}

	/** 闲聊轮次：{@code ok=true}、{@code result=null}、{@code text} 为模型原文 */
	public static ChatEnvelope chat(String business, String text, String version) {
		return new ChatEnvelope(true, business, version, null, text);
	}

	/** 闲聊轮次（契约版本取默认值） */
	public static ChatEnvelope chat(String business, String text) {
		return chat(business, text, DEFAULT_VERSION);
	}

	/** 失败轮次：{@code ok=false}、{@code result=null}、{@code text} 为兜底话术 */
	public static ChatEnvelope failure(String business, String text, String version) {
		return new ChatEnvelope(false, business, version, null, text);
	}

	/** 失败轮次（契约版本取默认值，兜底话术用 {@link #DEFAULT_FAILURE_TEXT}） */
	public static ChatEnvelope failure(String business, String text) {
		return failure(business, text, DEFAULT_VERSION);
	}

	/** 固定键清单（顺序即输出顺序） */
	public static List<String> keys() {
		return KEYS;
	}

	public boolean isOk() {
		return ok;
	}

	public String getBusiness() {
		return business;
	}

	/**
	 * 业务契约版本（信封键为 {@code v}）。
	 *
	 * <p>方法名按 Java 惯例叫 {@code getVersion()}，但**线上键名是 {@code v}**：已在 getter 上标注
	 * fastjson2 与 Jackson 的改名注解，直接序列化本对象也不会漏出 {@code version}。</p>
	 */
	@JSONField(name = KEY_VERSION)
	@JsonProperty(KEY_VERSION)
	public String getVersion() {
		return version;
	}

	/** 业务结构化结果副本；失败或闲聊轮次为 {@code null} */
	@JSONField(serializeFeatures = JSONWriter.Feature.WriteNulls)
	public JSONObject getResult() {
		return copy(result);
	}

	public String getText() {
		return text;
	}

	/**
	 * 转成对外输出的 {@link JSONObject}：每次返回新对象，恰好 5 键；
	 * {@code ok=false} 时 {@code result} 键依然存在（值为 {@code null}）。
	 */
	public JSONObject toJsonObject() {
		JSONObject json = new JSONObject();
		json.put(KEY_OK, ok);
		json.put(KEY_BUSINESS, business);
		json.put(KEY_VERSION, version);
		json.put(KEY_RESULT, copy(result));
		json.put(KEY_TEXT, text);
		return json;
	}

	/**
	 * 转成对外输出的 JSON 文本：恒好 5 键（{@code result} 为 {@code null} 时键也在），
	 * 供需要字符串形态的写出侧（日志、SSE {@code data}、自定义序列化器）使用。
	 */
	public String toJsonString() {
		return JSON.toJSONString(toJsonObject(), JSONWriter.Feature.WriteMapNullValue);
	}

	@Override
	public String toString() {
		return toJsonString();
	}

	private static JSONObject copy(JSONObject source) {
		return source == null ? null : new JSONObject((Map<String, Object>) source);
	}
}
