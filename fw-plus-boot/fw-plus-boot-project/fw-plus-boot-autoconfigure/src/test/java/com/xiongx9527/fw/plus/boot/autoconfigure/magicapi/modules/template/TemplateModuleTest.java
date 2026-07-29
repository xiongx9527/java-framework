package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.template;

import com.xiongx9527.fw.plus.template.TemplateParser;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class TemplateModuleTest {

    @Test
    void parseWordByUrlDelegatesToCorrectSpellingTemplateParserMethod() {
        RecordingTemplateParser parser = new RecordingTemplateParser();
        TemplateModule module = new TemplateModule(parser);

        byte[] result = module.parseWordByUrl("https://example.com/template.docx", Collections.singletonMap("name", "fw"));

        assertThat(result).isEqualTo(RecordingTemplateParser.WORD_URL_RESULT);
        assertThat(parser.parseWordByUrlCalled).isTrue();
        assertThat(parser.legacyParsWordByUrlCalled).isFalse();
        assertThat(parser.lastUrl).isEqualTo("https://example.com/template.docx");
        assertThat(parser.lastMap).containsEntry("name", "fw");
    }

    @Test
    void existingMethodsKeepDelegatingToTemplateParser() throws Exception {
        RecordingTemplateParser parser = new RecordingTemplateParser();
        TemplateModule module = new TemplateModule(parser);

        assertThat(module.parseWordByData(new byte[]{1}, null)).isEqualTo(RecordingTemplateParser.WORD_DATA_RESULT);
        assertThat(module.parseExcelByData(new byte[]{2}, null)).isEqualTo(RecordingTemplateParser.EXCEL_DATA_RESULT);
        assertThat(module.parseExcelByUrl("https://example.com/template.xlsx", null)).isEqualTo(RecordingTemplateParser.EXCEL_URL_RESULT);
        assertThat(module.parseHtmlByData(new byte[]{3}, null)).isEqualTo(RecordingTemplateParser.HTML_DATA_RESULT);
        assertThat(module.parseHtmlByUrl("https://example.com/template.html", null)).isEqualTo(RecordingTemplateParser.HTML_URL_RESULT);
    }

    @Test
    void defaultConstructorStillCreatesUsableModuleInstance() {
        assertThatCode(TemplateModule::new).doesNotThrowAnyException();
    }

    private static final class RecordingTemplateParser extends TemplateParser {
        private static final byte[] WORD_DATA_RESULT = new byte[]{11};
        private static final byte[] WORD_URL_RESULT = new byte[]{12};
        private static final byte[] EXCEL_DATA_RESULT = new byte[]{21};
        private static final byte[] EXCEL_URL_RESULT = new byte[]{22};
        private static final byte[] HTML_DATA_RESULT = new byte[]{31};
        private static final byte[] HTML_URL_RESULT = new byte[]{32};

        private boolean parseWordByUrlCalled;
        private boolean legacyParsWordByUrlCalled;
        private String lastUrl;
        private Map<String, Object> lastMap;

        private RecordingTemplateParser() {
            super(null, null, null);
        }

        @Override
        public byte[] parseWordByData(byte[] data, Map<String, Object> map) {
            return WORD_DATA_RESULT;
        }

        @Override
        public byte[] parseWordByUrl(String url, Map<String, Object> map) {
            this.parseWordByUrlCalled = true;
            this.lastUrl = url;
            this.lastMap = map;
            return WORD_URL_RESULT;
        }

        @Override
        public byte[] parsWordByUrl(String url, Map<String, Object> map) {
            this.legacyParsWordByUrlCalled = true;
            return new byte[]{99};
        }

        @Override
        public byte[] parseExcelByData(byte[] data, Map<String, Object> map) {
            return EXCEL_DATA_RESULT;
        }

        @Override
        public byte[] parseExcelByUrl(String url, Map<String, Object> map) {
            return EXCEL_URL_RESULT;
        }

        @Override
        public byte[] parseHtmlByData(byte[] data, Map<String, Object> map) {
            return HTML_DATA_RESULT;
        }

        @Override
        public byte[] parseHtmlByUrl(String url, Map<String, Object> map) {
            return HTML_URL_RESULT;
        }
    }
}
