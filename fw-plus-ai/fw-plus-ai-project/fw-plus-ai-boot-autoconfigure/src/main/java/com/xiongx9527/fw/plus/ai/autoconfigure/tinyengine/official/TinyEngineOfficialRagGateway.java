package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

import java.util.Map;

public interface TinyEngineOfficialRagGateway {
	TinyEngineOfficialAiResponse search(Map<String, Object> request);
}
