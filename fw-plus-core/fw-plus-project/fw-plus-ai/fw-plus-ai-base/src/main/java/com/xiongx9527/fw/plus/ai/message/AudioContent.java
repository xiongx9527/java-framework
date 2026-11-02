package com.xiongx9527.fw.plus.ai.message;

import com.xiongx9527.fw.plus.ai.data.audio.Audio;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * 音频消息内容（多模态输入）。
 *
 * <p><b>作用</b>：让消息携带音频（语音提问、录音转写前的原始输入）。音频以 {@link Audio}
 * 统一表达，URL / 原始字节 / base64 三种承载方式由 {@code Audio} 的构建器决定。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.data.message.AudioContent}
 * （{@code audio()} + {@code from(...)} 系列）。</p>
 *
 * @author xiongxiao
 */
public class AudioContent implements Content {

    private final Audio audio;

    /**
     * 由音频地址创建。
     *
     * @param url 音频地址
     */
    public AudioContent(URI url) {
        this(Audio.builder().url(url).build());
    }

    /**
     * 由音频地址字符串创建。
     *
     * @param url 音频地址
     */
    public AudioContent(String url) {
        this(URI.create(url));
    }

    /**
     * 由 base64 数据与 MIME 类型创建。
     *
     * @param base64Data base64 编码的音频
     * @param mimeType   MIME 类型
     */
    public AudioContent(String base64Data, String mimeType) {
        this(Audio.builder().base64Data(base64Data).mimeType(mimeType).build());
    }

    /**
     * 由 {@link Audio} 创建。
     *
     * @param audio 音频
     */
    public AudioContent(Audio audio) {
        if (audio == null) {
            throw new IllegalArgumentException("audio must not be null");
        }
        this.audio = audio;
    }

    @Override
    public ContentType type() {
        return ContentType.AUDIO;
    }

    /**
     * 音频。
     *
     * @return 音频对象，永不为 {@code null}
     */
    public Audio audio() {
        return audio;
    }

    @Override
    public String toString() {
        return "AudioContent{audio=" + audio + '}';
    }

    /**
     * 由音频地址创建。
     *
     * @param url 音频地址
     * @return 音频内容
     */
    public static AudioContent from(URI url) {
        return new AudioContent(url);
    }

    /**
     * 由音频地址字符串创建。
     *
     * @param url 音频地址
     * @return 音频内容
     */
    public static AudioContent from(String url) {
        return new AudioContent(url);
    }

    /**
     * 由 base64 数据与 MIME 类型创建。
     *
     * @param base64Data base64 编码的音频
     * @param mimeType   MIME 类型
     * @return 音频内容
     */
    public static AudioContent from(String base64Data, String mimeType) {
        return new AudioContent(base64Data, mimeType);
    }

    /**
     * 由本地文件创建（读取为 base64）。
     *
     * @param path     文件路径
     * @param mimeType MIME 类型
     * @return 音频内容
     */
    public static AudioContent from(Path path, String mimeType) {
        try {
            byte[] bytes = Files.readAllBytes(path);
            return new AudioContent(Base64.getEncoder().encodeToString(bytes), mimeType);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read audio from " + path, e);
        }
    }

    /**
     * 由 {@link Audio} 创建。
     *
     * @param audio 音频
     * @return 音频内容
     */
    public static AudioContent from(Audio audio) {
        return new AudioContent(audio);
    }
}
