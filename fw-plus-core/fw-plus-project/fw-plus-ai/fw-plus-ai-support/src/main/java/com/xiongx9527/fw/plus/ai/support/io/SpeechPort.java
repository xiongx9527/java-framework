package com.xiongx9527.fw.plus.ai.support.io;

/**
 * 语音端口：文本转语音与语音转文本。
 *
 * <p><b>作用</b>：语音只是交互通道，不应进入问数主链。端口把「合成」与「识别」两件事抽象出来，
 * 由实现选择具体服务（厂商 ASR / TTS、本地模型）。问数链路只负责把识别出的文本当作用户问题、
 * 把回答文本送去合成。</p>
 *
 * <p><b>出处</b>：C 类（自行抽象）。对应 {@code docs/ai-layer/ai-port-layer.md} §6.18.10 SpeechPort
 * 与增强清单「语音 ASR / TTS」。langchain4j 有 {@code SpeechModel}（1.x），Spring AI 无统一抽象，
 * 两者形态不一，故此处只保留最小语义。</p>
 *
 * <p><b>实现约定</b>：</p>
 * <ul>
 *   <li>实现必须无状态；并发调用安全。</li>
 *   <li>识别失败抛 {@link com.xiongx9527.fw.plus.ai.exception.IllegalConfigurationException} 或返回空串，
 *       由调用方决定是否降级为纯文本交互。</li>
 *   <li>音频格式以入参显式声明，不做隐式探测。</li>
 * </ul>
 *
 * @author xiongxiao
 */
public interface SpeechPort {

    /**
     * 语音合成。
     *
     * @param text   待合成文本
     * @param voice  音色标识，{@code null} 表示默认音色
     * @param format 输出音频格式（如 {@code mp3} / {@code wav}）
     * @return 音频字节
     */
    byte[] synthesize(String text, String voice, String format);

    /**
     * 语音识别。
     *
     * @param audio  音频字节
     * @param format 音频格式（如 {@code wav} / {@code mp3}）
     * @return 识别文本
     */
    String transcribe(byte[] audio, String format);
}
