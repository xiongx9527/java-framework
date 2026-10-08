package com.xiongx9527.fw.plus.dify;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.dify.app.IAppBaseClient;
import com.xiongx9527.fw.plus.dify.support.RecordingHttpServer;
import com.xiongx9527.fw.plus.dify.types.AudioFile;
import com.xiongx9527.fw.plus.dify.types.DifyClientException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;

/**
 * 语音接口对齐测试（brief A9）：audioToText 的 30MB 边界与受支持 MIME 集合本地校验、
 * textToAudio 的 voice 重载与 message_id 优先语义。
 * 不依赖 JUnit，由 Surefire 的 POJO 测试发现机制执行。
 */
public class AudioAlignmentContractTest {

    private static final long MAX_BYTES = 30L * 1024 * 1024;

    public void testAudioToTextRejectsFileLargerThan30Mb() throws Exception {
        File oversize = sparseFile("audio-oversize", ".mp3", MAX_BYTES + 1);
        IAppBaseClient base = DifyClientBuilder.app("http://127.0.0.1:1", "app-key").base().build();
        try {
            base.audioToText(oversize, "u1", "audio/mp3");
            throw new AssertionError("expected DifyClientException(audios_too_large)");
        } catch (DifyClientException e) {
            assertTrue(e.getMessage().contains("audio_too_large"),
                    "超限错误含 audio_too_large，实际=" + e.getMessage());
        } finally {
            oversize.delete();
        }
    }

    public void testAudioToTextAcceptsExactly30Mb() throws Exception {
        File boundary = sparseFile("audio-boundary", ".mp3", MAX_BYTES);
        RecordingHttpServer server = new RecordingHttpServer(
                RecordingHttpServer.json(200, "{\"text\":\"ok\"}")
        ).start();
        try {
            IAppBaseClient base = DifyClientBuilder.app(server.baseUrl(), "app-key").base().build();
            String text = base.audioToText(boundary, "u1");
            assertEquals("ok", text, "30MB 边界文件转写成功");
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("POST", recorded.method, "audio-to-text method");
            assertEquals("/audio-to-text", recorded.path, "POST /audio-to-text path");
            assertTrue(recorded.body.contains("Content-Type: audio/mp3"),
                    "multipart 文件部分 Content-Type=audio/mp3");
        } finally {
            server.close();
            boundary.delete();
        }
    }

    public void testAudioToTextAcceptsDocumentedMimeSet() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(
                RecordingHttpServer.json(200, "{\"text\":\"hello\"}")
        ).start();
        try {
            IAppBaseClient base = DifyClientBuilder.app(server.baseUrl(), "app-key").base().build();

            // 后缀推断集合：mp3 / m4a / wav / amr / mpga
            String[][] suffixes = new String[][] {
                    {".mp3", "audio/mp3"},
                    {".m4a", "audio/m4a"},
                    {".wav", "audio/wav"},
                    {".amr", "audio/amr"},
                    {".mpga", "audio/mpga"}
            };
            for (String[] suffix : suffixes) {
                File file = tempFile("audio-ok", suffix[0], "fake-audio");
                try {
                    String text = base.audioToText(file, "u1");
                    assertEquals("hello", text, "MIME " + suffix[1] + " 接受");
                    assertTrue(server.last().body.contains("Content-Type: " + suffix[1]),
                            "multipart Content-Type=" + suffix[1]);
                } finally {
                    file.delete();
                }
            }

            // 显式 audio/x-m4a（m4a 的变体）被接受
            File m4a = tempFile("audio-explicit", ".bin", "fake-audio");
            try {
                base.audioToText(m4a, "u1", "audio/x-m4a");
                assertTrue(server.last().body.contains("Content-Type: audio/x-m4a"),
                        "显式 audio/x-m4a 接受");
            } finally {
                m4a.delete();
            }
        } finally {
            server.close();
        }
    }

    public void testAudioToTextRejectsUnsupportedMime() throws Exception {
        IAppBaseClient base = DifyClientBuilder.app("http://127.0.0.1:1", "app-key").base().build();

        // audio/mpeg 不在接受集合内（按文档拒绝）
        File mpeg = tempFile("audio-mpeg", ".mpeg", "fake");
        try {
            expectUnsupported(base, mpeg, null, "后缀 mpeg 拒绝");
        } finally {
            mpeg.delete();
        }

        File mp3 = tempFile("audio-mp3", ".mp3", "fake");
        try {
            expectUnsupported(base, mp3, "audio/mpeg", "显式 audio/mpeg 拒绝");
            expectUnsupported(base, mp3, "audio/mp4", "显式 audio/mp4 拒绝");
            expectUnsupported(base, mp3, "audio/webm", "显式 audio/webm 拒绝");
        } finally {
            mp3.delete();
        }

        // 无后缀无法推断 → 拒绝
        File noExt = tempFile("audio-noext", "", "fake");
        try {
            expectUnsupported(base, noExt, null, "无后缀拒绝");
        } finally {
            noExt.delete();
        }

        // 文件不存在
        try {
            base.audioToText(new File("does-not-exist-audio.mp3"), "u1");
            throw new AssertionError("expected DifyClientException(does not exist)");
        } catch (DifyClientException e) {
            assertTrue(e.getMessage().contains("does not exist"), "不存在文件报错");
        }
    }

    public void testTextToAudioVoiceOverloadAndMessageIdPriority() throws Exception {
        RecordingHttpServer server = new RecordingHttpServer(
                RecordingHttpServer.bytes(200, "audio/mpeg", "ID3fake".getBytes(StandardCharsets.UTF_8))
        ).start();
        try {
            IAppBaseClient base = DifyClientBuilder.app(server.baseUrl(), "app-key").base().build();

            // voice 重载：body 携带 message_id + text + user + voice（message_id 与 text 同传时优先）
            AudioFile withVoice = base.textToAudio("u1", "hello", "msg-1", "female-1");
            RecordingHttpServer.Recorded recorded = server.last();
            assertEquals("POST", recorded.method, "text-to-audio method");
            assertEquals("/text-to-audio", recorded.path, "POST /text-to-audio path");
            JSONObject body = JSON.parseObject(recorded.body);
            assertEquals("female-1", body.getString("voice"), "body voice");
            assertEquals("msg-1", body.getString("message_id"), "body message_id");
            assertEquals("hello", body.getString("text"), "body text");
            assertEquals("u1", body.getString("user"), "body user");
            assertEquals("audio/mpeg", withVoice.getType(), "AudioFile Content-Type");
            assertTrue(withVoice.getData() != null && withVoice.getData().length > 0, "AudioFile 字节");

            // 旧签名保留：voice/message_id 为空时不携带
            base.textToAudio("u1", "hello");
            body = JSON.parseObject(server.last().body);
            assertEquals(null, body.getString("voice"), "未传 voice 时不携带");
            assertEquals(null, body.getString("message_id"), "未传 message_id 时不携带");
            assertEquals("hello", body.getString("text"), "text 保留");
        } finally {
            server.close();
        }
    }

    private static void expectUnsupported(IAppBaseClient base, File file, String fileType, String message) {
        try {
            base.audioToText(file, "u1", fileType);
            throw new AssertionError("expected DifyClientException(unsupported_audio_type): " + message);
        } catch (DifyClientException e) {
            assertTrue(e.getMessage().contains("unsupported_audio_type"),
                    message + "，实际=" + e.getMessage());
        }
    }

    private static File sparseFile(String prefix, String suffix, long length) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        RandomAccessFile raf = new RandomAccessFile(file, "rw");
        try {
            raf.setLength(length);
        } finally {
            raf.close();
        }
        return file;
    }

    private static File tempFile(String prefix, String suffix, String content) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        file.deleteOnExit();
        FileOutputStream out = new FileOutputStream(file);
        try {
            out.write(content.getBytes(StandardCharsets.UTF_8));
        } finally {
            out.close();
        }
        return file;
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " expected:<" + expected + "> but was:<" + actual + ">");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
