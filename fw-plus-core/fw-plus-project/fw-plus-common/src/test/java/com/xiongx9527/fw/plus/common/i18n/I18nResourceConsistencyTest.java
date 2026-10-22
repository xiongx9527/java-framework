package com.xiongx9527.fw.plus.common.i18n;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 国际化资源文件一致性测试。
 *
 * <p>校验英文资源与中文默认资源（{@code messages.properties}）的 key 集合一致，
 * 避免新增 key 后漏翻英文；并校验编码为 UTF-8。</p>
 *
 * <p>资源目录相对当前模块根目录（Maven 以模块根为工作目录运行测试）。</p>
 */
@DisplayName("i18n 资源文件一致性")
class I18nResourceConsistencyTest {

    /** 资源目录相对本模块根目录 */
    private static final String I18N_DIR = "src/main/resources/i18n";

    @Test
    @DisplayName("英文资源 key 集合与中文默认资源完全一致")
    void englishKeysMatchChinese() throws Exception {
        File zh = new File(I18N_DIR, "messages.properties");
        File en = new File(I18N_DIR, "messages_en.properties");
        assertTrue(zh.isFile(), "缺少中文默认资源: " + zh.getPath());
        assertTrue(en.isFile(), "缺少英文资源: " + en.getPath());

        Set<String> zhKeys = keysOf(zh);
        Set<String> enKeys = keysOf(en);

        Set<String> missingInEn = new LinkedHashSet<>(zhKeys);
        missingInEn.removeAll(enKeys);
        Set<String> missingInZh = new LinkedHashSet<>(enKeys);
        missingInZh.removeAll(zhKeys);

        assertTrue(missingInEn.isEmpty(), "英文资源缺少以下 key: " + missingInEn);
        assertTrue(missingInZh.isEmpty(), "中文资源缺少以下 key: " + missingInZh);
    }

    @Test
    @DisplayName("资源文件以 UTF-8 存储且不含乱码替换字符")
    void resourcesAreUtf8() throws Exception {
        for (String name : new String[]{"messages.properties", "messages_en.properties"}) {
            File f = new File(I18N_DIR, name);
            assertTrue(f.isFile(), "缺少资源: " + f.getPath());
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
            assertFalse(content.contains("\uFFFD"), name + " 存在 UTF-8 解码替换字符，可能编码错误");
        }
    }

    @Test
    @DisplayName("中文默认资源含中文文案")
    void chineseResourceContainsChinese() throws Exception {
        File zh = new File(I18N_DIR, "messages.properties");
        String content = new String(Files.readAllBytes(zh.toPath()), StandardCharsets.UTF_8);
        assertTrue(Pattern.compile("[\u4e00-\u9fa5]").matcher(content).find(),
                "中文默认资源未发现中文字符");
    }

    private static Set<String> keysOf(File file) throws Exception {
        Properties props = new Properties();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            props.load(reader);
        }
        return new LinkedHashSet<>(props.stringPropertyNames());
    }
}
