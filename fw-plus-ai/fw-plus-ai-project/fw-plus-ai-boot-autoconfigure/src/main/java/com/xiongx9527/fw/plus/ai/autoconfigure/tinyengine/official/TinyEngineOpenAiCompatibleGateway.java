package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

public interface TinyEngineOpenAiCompatibleGateway {
	TinyEngineOfficialAiResponse execute(TinyEngineOfficialAiRequest request);

	void stream(TinyEngineOfficialAiRequest request, TinyEngineSseConsumer consumer);
}
