package com.xiongx9527.fw.plus.ai.sample.structured.envelope;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.ChatEnvelope;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 两条通道同形断言：同一轮次，流式通道 {@code done} 帧承载的信封与非流式入口返回的 {@link ChatEnvelope}
 * 逐键逐值一致（键结构、{@code ok}、{@code business}、{@code v}、{@code result}、{@code text}）。
 *
 * <p><b>帧是怎么拿到的</b>：{@code SseEmitter} 由 {@code SimpleAssistant} 内部构造、
 * {@code ResponseBodyEmitter.initialize(Handler)} 又是包私有方法，但<b>未初始化时每一帧都缓冲在私有字段
 * {@code earlySendAttempts}</b>（{@code LinkedHashSet}，保持发送顺序）里，反射即可读到<b>真实帧</b>：
 * 一个事件是「前缀（{@code event:xxx\nid:xxx\ndata:}）+ 数据本体（{@code mediaType == null}）+ 终止片段」。
 * 因此本测试既不依赖真实 HTTP 连接，也不复刻框架生成信封的表达式——{@code done} 帧里装的是什么，
 * 断言的就是什么（把框架的信封分支改成回模型原文，本测试必失败）。</p>
 *
 * <p><b>两个限制</b>：</p>
 * <ol>
 *   <li>帧的数据本体是<b>对象</b>（{@code start}/{@code done} 帧为 {@code JSONObject}、{@code token} 帧为
 *       {@code String}），线上字节由宿主的 {@code HttpMessageConverter} 决定；本测试断言对象层面的键与值。
 *       取键时<b>不要</b>先 {@code toString()}：fastjson2 默认序列化会丢掉 {@code null} 值键，
 *       失败轮次的 {@code result} 键会“看不见”（对象层面它是存在的）。</li>
 *   <li>假模型在请求内同步回调，{@code streamToSse(...)} 返回时帧已全部缓冲；真实异步流不满足这一点，
 *       本测试的取帧手法仅适用于离线样例。</li>
 * </ol>
 */
class EnvelopeChannelSameShapeTest {

	private static final String ORDER_MESSAGE = "帮我查订单 A1001 的付款状态";

	private static final String BAD_ORDER_MESSAGE = "帮我查订单 BAD 的付款状态";

	private static final String CHAT_MESSAGE = "今天天气怎么样";

	/** 事件前缀的结尾：其后紧跟的就是该事件的 {@code data:} 行数据本体 */
	private static final String DATA_MARKER = "\ndata:";

	private final EnvelopeAssistantSample sample = new EnvelopeAssistantSample();

	// -------------------------------------------------- 闲聊轮次：逐字下发，done 帧 data 是信封

	@Test
	void chatRoundDoneFrameDataEqualsNonStreamingEnvelope() {
		String streamingId = "same-chat-stream";
		String nonStreamingId = "same-chat-entry";

		List<Frame> frames = frames(sample.assistant().streamToSse(streamingId, CHAT_MESSAGE));

		// 闲聊轮次逐字下发：start → token → token → done
		assertThat(names(frames)).containsExactly("start", "token", "token", "done");
		assertThat(sample.streamingModel().requestCount()).as("token 帧唯一的来源被调用一次").isEqualTo(1);

		// start 帧 data：三键，本轮逐字下发
		JSONObject startData = dataOf(frames, 0);
		assertThat(startData.keySet()).containsExactlyInAnyOrder("requestId", "business", "tokens");
		assertThat(startData.getString("requestId")).hasSize(32);
		assertThat(startData.getBooleanValue("tokens")).isTrue();

		// token 帧的 data 是纯文本分片（不是信封），且与流式模型实际回调的分片逐字一致
		assertThat(texts(frames)).containsExactlyElementsOf(sample.streamingModel().deliveredTokens());

		// done 帧 data = 非流式入口返回的信封，逐键逐值
		JSONObject doneData = dataOf(frames, 3);
		ChatEnvelope envelope = sample.entry().chat(nonStreamingId, CHAT_MESSAGE);
		assertSameEnvelope(doneData, envelope);
		assertThat(doneData.getString(ChatEnvelope.KEY_VERSION)).isEqualTo(ChatEnvelope.DEFAULT_VERSION);
		assertThat(doneData.getString(ChatEnvelope.KEY_TEXT)).as("done 帧的正文 = 逐字下发拼起来的正文")
				.isEqualTo(String.join("", texts(frames)));
		assertThat(startData.getString("business")).isEqualTo(envelope.getBusiness());

		// 所有事件的 id 都与 start 帧 data 里的 requestId 一致
		assertThat(ids(frames)).containsOnly(startData.getString("requestId"));
	}

	// -------------------------------------------------- 结构化轮次：不逐字下发，done 帧 data 是信封

	@Test
	void structuredRoundDoneFrameDataEqualsNonStreamingEnvelope() {
		String streamingId = "same-order-stream";
		String nonStreamingId = "same-order-entry";

		List<Frame> frames = frames(sample.assistant().streamToSse(streamingId, ORDER_MESSAGE));

		// 结构化轮次不发 token 帧：只有 start 与 done 两个事件，且没有纯文本的数据本体
		assertThat(names(frames)).containsExactly("start", "done");
		assertThat(texts(frames)).as("结构化轮次的帧里没有任何纯文本数据（没有 token 帧）").isEmpty();
		// 与「帧里没有 token」互为佐证：token 帧唯一的来源一次都没被调用
		assertThat(sample.streamingModel().requestCount()).isZero();

		JSONObject startData = dataOf(frames, 0);
		assertThat(startData.keySet()).containsExactlyInAnyOrder("requestId", "business", "tokens");
		assertThat(startData.getString("requestId")).hasSize(32);
		assertThat(startData.getBooleanValue("tokens")).isFalse();
		assertThat(startData.getString("business")).isEqualTo(SampleBusinesses.ORDER);

		// done 帧 data = 非流式入口返回的信封，逐键逐值（v、result 都取自真实帧，不再二次分类）
		JSONObject doneData = dataOf(frames, 1);
		ChatEnvelope envelope = sample.entry().chat(nonStreamingId, ORDER_MESSAGE);
		assertThat(envelope.isOk()).isTrue();
		assertSameEnvelope(doneData, envelope);
		assertThat(doneData.getString(ChatEnvelope.KEY_VERSION)).isEqualTo("1");
		assertThat(doneData.getString(ChatEnvelope.KEY_TEXT)).as("结构化成功轮次的 text 为空串").isEmpty();
		assertThat(doneData.getJSONObject(ChatEnvelope.KEY_RESULT).getString("orderId")).isEqualTo("A1001");

		assertThat(ids(frames)).containsOnly(startData.getString("requestId"));
	}

	@Test
	void failedStructuredRoundDoneFrameKeepsEnvelopeShape() {
		List<Frame> frames = frames(sample.assistant().streamToSse("same-order-bad", BAD_ORDER_MESSAGE));

		assertThat(names(frames)).containsExactly("start", "done");
		JSONObject doneData = dataOf(frames, 1);
		JSONObject startData = dataOf(frames, 0);

		ChatEnvelope envelope = sample.entry().chat("same-order-bad-entry", BAD_ORDER_MESSAGE);
		assertThat(envelope.isOk()).as("契约校验不过：两条通道都是失败信封").isFalse();
		assertSameEnvelope(doneData, envelope);
		assertThat(doneData).as("失败也保持 5 键：result 键在、值为 null").containsKey(ChatEnvelope.KEY_RESULT);
		assertThat(doneData.get(ChatEnvelope.KEY_RESULT)).isNull();
		assertThat(doneData.getString(ChatEnvelope.KEY_TEXT)).isEqualTo(ChatEnvelope.DEFAULT_FAILURE_TEXT);

		assertThat(ids(frames)).containsOnly(startData.getString("requestId"));
	}

	// -------------------------------------------------- 工具：读取真实帧

	/**
	 * 读出未初始化时缓冲的真实帧：{@code ResponseBodyEmitter} 把每一帧（含事件前缀与终止片段）按发送顺序
	 * 放进私有字段 {@code earlySendAttempts}。前缀/终止片段的 {@code mediaType} 非 {@code null}，
	 * {@code data:} 行的数据本体（{@code start}/{@code done} 帧为 {@code JSONObject}、{@code token} 帧为
	 * {@code String}）的 {@code mediaType} 为 {@code null}。
	 */
	private static List<Frame> frames(SseEmitter emitter) {
		List<Element> elements = bufferedElements(emitter);
		List<Frame> frames = new ArrayList<Frame>();
		for (int i = 0; i < elements.size(); i++) {
			Element element = elements.get(i);
			if (element.mediaType != null) {
				continue;
			}
			frames.add(frameOf(element.data, elements, i));
		}
		return frames;
	}

	/** 数据本体的前一个片段就是它的事件前缀；对不上即说明 Spring 内部结构变了，直接失败而不是静默通过 */
	private static Frame frameOf(Object data, List<Element> elements, int index) {
		Object previous = index > 0 ? elements.get(index - 1).data : null;
		if (!(previous instanceof String) || !((String) previous).endsWith(DATA_MARKER)) {
			throw new IllegalStateException("帧结构意外：数据本体之前不是以 " + DATA_MARKER + " 结尾的事件前缀");
		}
		return new Frame(markerValue((String) previous, "event:"), markerValue((String) previous, "id:"), data);
	}

	private static List<Element> bufferedElements(SseEmitter emitter) {
		try {
			Field field = ResponseBodyEmitter.class.getDeclaredField("earlySendAttempts");
			field.setAccessible(true);
			Collection<?> attempts = (Collection<?>) field.get(emitter);
			List<Element> elements = new ArrayList<Element>();
			for (Object attempt : attempts) {
				Method getData = attempt.getClass().getMethod("getData");
				Method getMediaType = attempt.getClass().getMethod("getMediaType");
				getData.setAccessible(true);
				getMediaType.setAccessible(true);
				elements.add(new Element(getData.invoke(attempt), getMediaType.invoke(attempt)));
			}
			return elements;
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("读取 SSE 帧缓冲失败（Spring 内部结构可能变了）", e);
		}
	}

	/** 前缀里 {@code event:xxx} / {@code id:xxx} 之后到换行为止的值 */
	private static String markerValue(String prefix, String marker) {
		int start = prefix.indexOf(marker);
		if (start < 0) {
			throw new IllegalStateException("帧结构意外：前缀里没有 " + marker + "：" + prefix);
		}
		start += marker.length();
		int end = prefix.indexOf('\n', start);
		return end < 0 ? prefix.substring(start) : prefix.substring(start, end);
	}

	private static List<String> names(List<Frame> frames) {
		List<String> names = new ArrayList<String>();
		for (Frame frame : frames) {
			names.add(frame.name);
		}
		return names;
	}

	private static List<String> ids(List<Frame> frames) {
		List<String> ids = new ArrayList<String>();
		for (Frame frame : frames) {
			ids.add(frame.id);
		}
		return ids;
	}

	/** 纯文本的数据本体（只有 {@code token} 帧是这种形态） */
	private static List<String> texts(List<Frame> frames) {
		List<String> texts = new ArrayList<String>();
		for (Frame frame : frames) {
			if (frame.data instanceof String) {
				texts.add((String) frame.data);
			}
		}
		return texts;
	}

	/** 第 index 个事件的数据本体，按信封 JSON 取；不是信封（如被改回模型原文）时抛 ClassCastException */
	private static JSONObject dataOf(List<Frame> frames, int index) {
		return (JSONObject) frames.get(index).data;
	}

	/** 真实 done 帧 data 与非流式入口的信封逐键逐值比较 */
	private static void assertSameEnvelope(JSONObject streamingData, ChatEnvelope envelope) {
		JSONObject entryData = envelope.toJsonObject();
		assertThat(streamingData.keySet()).as("done 帧 data 的键结构与 5 键一致（含顺序）")
				.containsExactlyElementsOf(ChatEnvelope.keys());
		assertThat(entryData.keySet()).containsExactlyElementsOf(ChatEnvelope.keys());
		assertThat(streamingData.getBooleanValue(ChatEnvelope.KEY_OK)).isEqualTo(envelope.isOk());
		assertThat(streamingData.getString(ChatEnvelope.KEY_BUSINESS)).isEqualTo(envelope.getBusiness());
		assertThat(streamingData.getString(ChatEnvelope.KEY_VERSION)).isEqualTo(envelope.getVersion());
		assertThat(streamingData.get(ChatEnvelope.KEY_RESULT)).isEqualTo(entryData.get(ChatEnvelope.KEY_RESULT));
		assertThat(streamingData.getString(ChatEnvelope.KEY_TEXT)).isEqualTo(envelope.getText());
		if (entryData.get(ChatEnvelope.KEY_RESULT) == null) {
			assertThat(streamingData).as("result 为 null 时键仍必须在").containsKey(ChatEnvelope.KEY_RESULT);
		}
	}

	/** 帧缓冲里的一个原始片段 */
	private static final class Element {

		private final Object data;

		private final Object mediaType;

		private Element(Object data, Object mediaType) {
			this.data = data;
			this.mediaType = mediaType;
		}
	}

	/** 一个已解析的 SSE 事件（真实帧） */
	private static final class Frame {

		private final String name;

		private final String id;

		private final Object data;

		private Frame(String name, String id, Object data) {
			this.name = name;
			this.id = id;
			this.data = data;
		}
	}
}
