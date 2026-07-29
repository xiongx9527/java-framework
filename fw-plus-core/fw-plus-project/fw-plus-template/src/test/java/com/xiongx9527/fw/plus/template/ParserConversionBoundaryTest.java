package com.xiongx9527.fw.plus.template;

import com.xiongx9527.fw.plus.template.excel.ExcelParser;
import com.xiongx9527.fw.plus.template.html.HtmlParser;
import com.xiongx9527.fw.plus.template.word.WordParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParserConversionBoundaryTest {

    @Test
    void wordParserFailsClearlyWhenWordToHtmlConverterMissing() {
        assertThatThrownBy(() -> new WordParser(null, null).toHtml(new byte[]{1}))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Word to Html conversion is not supported");
    }

    @Test
    void wordParserFailsClearlyWhenWordToPdfConverterMissing() {
        assertThatThrownBy(() -> new WordParser(null, null).toPdf(new byte[]{1}))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Word to Pdf conversion is not supported");
    }

    @Test
    void excelParserFailsClearlyWhenExcelToHtmlConverterMissing() {
        assertThatThrownBy(() -> new ExcelParser(null).toHtml(new byte[]{1}))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Excel to Html conversion is not supported");
    }

    @Test
    void excelParserKeepsPdfUnsupported() {
        assertThatThrownBy(() -> new ExcelParser(null).toPdf(new byte[]{1}))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Excel to Pdf conversion is not supported");
    }

    @Test
    void htmlParserFailsClearlyWhenHtmlToPdfConverterMissing() {
        assertThatThrownBy(() -> new HtmlParser(null).toPdf(new byte[]{1}))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Html to Pdf conversion is not supported");
    }
}
