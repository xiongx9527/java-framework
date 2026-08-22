package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.official;

@FunctionalInterface
public interface TinyEngineSseConsumer {

	void accept(byte[] bytes);

	default void onSession(TinyEngineSseSession session) {
	}
}
