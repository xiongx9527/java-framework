package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 演示工具调用：查询城市天气。
 *
 * <p>数据为本地构造的 mock，仅用于演示"模型自主决定是否调用工具"这一行为，
 * 不访问任何外部天气服务。由模型自行决定何时调用、传什么参数——这正是
 * LangChain4j 工具调用的核心特征。</p>
 *
 * <p>同一方法同时标注 LangChain4j 的 {@code @Tool} 与 Spring AI 的
 * {@code @Tool}（全限定名），使这一个工具类同时服务于 LangChain4j、
 * Spring AI、Spring AI Alibaba 三条链路，调用计数也是同一份。</p>
 */
@Slf4j
public class WeatherTools {

	/** 调用计数，用于在接口响应中回报本次是否触发了工具。 */
	private final AtomicInteger callCount = new AtomicInteger(0);

	/** mock 数据：城市名 → [温度(℃), 湿度(%), 风力(级), 描述]。 */
	private static final Map<String, String[]> MOCK_WEATHER = new LinkedHashMap<>();

	static {
		MOCK_WEATHER.put("北京", new String[] { "32", "45", "3", "晴，午后有雷阵雨" });
		MOCK_WEATHER.put("杭州", new String[] { "29", "78", "2", "多云转阴，闷热" });
		MOCK_WEATHER.put("深圳", new String[] { "33", "82", "2", "阵雨" });
	}

	@Tool("查询指定城市的实时天气，返回温度、湿度、风力和天气描述")
	@org.springframework.ai.tool.annotation.Tool(name = "queryWeather",
			description = "查询指定城市的实时天气，返回温度、湿度、风力和天气描述")
	public String queryWeather(@P("城市名，例如：北京")
			@org.springframework.ai.tool.annotation.ToolParam(description = "城市名，例如：北京") String city) {
		log.info("=======================>查询天气：{}", city);
		callCount.incrementAndGet();
		String[] data = MOCK_WEATHER.get(city == null ? "" : city.trim());
		if (data == null) {
			// 未覆盖的城市返回空结果，让模型自行决定如何回复
			return "{\"city\":\"" + city + "\",\"found\":false,\"message\":\"暂无该城市的天气数据\"}";
		}
		return "{\"city\":\"" + city.trim() + "\",\"found\":true,"
				+ "\"temperatureC\":" + data[0] + ","
				+ "\"humidity\":" + data[1] + ","
				+ "\"windLevel\":" + data[2] + ","
				+ "\"description\":\"" + data[3] + "\"}";
	}

	/** 返回累计调用次数。 */
	public int getCallCount() {
		return callCount.get();
	}

	/** 清零计数（测试用）。 */
	public void reset() {
		callCount.set(0);
	}
}
