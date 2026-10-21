package com.xiongx9527.fw.plus.template;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * fw-plus-template 模块异常文案的国际化测试。
 *
 * <p>该模块抛的是 JDK 异常（保持类型不变），文案使用具名 dotted key。</p>
 */
@DisplayName("template 异常国际化文案")
class TemplateExceptionI18nTest {

    private static final Locale ZH = Locale.SIMPLIFIED_CHINESE;
    private static final Locale EN = Locale.US;

    @AfterEach
    void reset() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("无法解析 Word 文档：中文 / 英文")
    void unsupportedWordFormat() {
        LocaleContextHolder.setLocale(ZH);
        assertEquals("无法解析Word文档，请确认文件格式为 .docx（OOXML）",
                I18nMessageResolver.resolveKey("fw.template.WordParser.unsupportedWordFormat"));
        LocaleContextHolder.setLocale(EN);
        assertEquals("Unable to parse the Word document, please make sure the format is .docx (OOXML)",
                I18nMessageResolver.resolveKey("fw.template.WordParser.unsupportedWordFormat"));
    }

    @Test
    @DisplayName("无法解析 Excel 文件：中文 / 英文")
    void excelParseFailed() {
        LocaleContextHolder.setLocale(ZH);
        assertEquals("无法解析Excel文件",
                I18nMessageResolver.resolveKey("fw.template.EasyPoiExcel2Html.excelParseFailed"));
        LocaleContextHolder.setLocale(EN);
        assertEquals("Unable to parse the Excel file",
                I18nMessageResolver.resolveKey("fw.template.EasyPoiExcel2Html.excelParseFailed"));
    }

    @Test
    @DisplayName("未知 key 解析为 null，不抛异常")
    void unknownKeyReturnsNull() {
        assertNull(I18nMessageResolver.resolveKey("fw.template.WordParser.noSuchCase"));
    }
}
