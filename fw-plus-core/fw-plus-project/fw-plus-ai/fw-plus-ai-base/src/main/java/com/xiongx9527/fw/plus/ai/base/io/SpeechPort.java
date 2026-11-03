package com.xiongx9527.fw.plus.ai.base.io;

/**
 * 语音端口：语音合成与识别。
 */
public interface SpeechPort {

    /** 文本转语音。 */
    byte[] synthesize(String text, String voice);

    /** 语音转文本。 */
    String transcribe(byte[] audio, String format);
}
