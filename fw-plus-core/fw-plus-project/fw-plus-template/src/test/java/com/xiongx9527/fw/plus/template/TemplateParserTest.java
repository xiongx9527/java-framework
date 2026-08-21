package com.xiongx9527.fw.plus.template;

import com.xiongx9527.fw.plus.template.excel.ExcelAbstractParser;
import com.xiongx9527.fw.plus.template.html.HtmlAbstractParser;
import com.xiongx9527.fw.plus.template.word.WordAbstractParser;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateParserTest {

    @Test
    void parseWordByUrlDelegatesToWordParser() {
        RecordingWordParser wordParser = new RecordingWordParser();
        TemplateParser parser = new TemplateParser(wordParser, null, null);

        byte[] result = parser.parseWordByUrl("https://example.com/template.docx", Collections.singletonMap("name", "fw"));

        assertThat(result).isEqualTo(RecordingWordParser.URL_RESULT);
        assertThat(wordParser.lastUrl).isEqualTo("https://example.com/template.docx");
        assertThat(wordParser.lastMap).containsEntry("name", "fw");
    }

    @Test
    void legacyParsWordByUrlDelegatesToCorrectSpellingEntry() {
        RecordingWordParser wordParser = new RecordingWordParser();
        TemplateParser parser = new TemplateParser(wordParser, null, null);

        byte[] result = parser.parsWordByUrl("https://example.com/legacy.docx", null);

        assertThat(result).isEqualTo(RecordingWordParser.URL_RESULT);
        assertThat(wordParser.lastUrl).isEqualTo("https://example.com/legacy.docx");
        assertThat(wordParser.lastMap).isEmpty();
    }

    @Test
    void dataEntriesDelegateAndConvertNullMapToEmptyMap() {
        RecordingWordParser wordParser = new RecordingWordParser();
        RecordingExcelParser excelParser = new RecordingExcelParser();
        RecordingHtmlParser htmlParser = new RecordingHtmlParser();
        TemplateParser parser = new TemplateParser(wordParser, excelParser, htmlParser);

        assertThat(parser.parseWordByData(new byte[]{1}, null)).isEqualTo(RecordingWordParser.DATA_RESULT);
        assertThat(parser.parseExcelByData(new byte[]{2}, null)).isEqualTo(RecordingExcelParser.DATA_RESULT);
        assertThat(parser.parseHtmlByData(new byte[]{3}, null)).isEqualTo(RecordingHtmlParser.DATA_RESULT);

        assertThat(wordParser.lastMap).isEmpty();
        assertThat(excelParser.lastMap).isEmpty();
        assertThat(htmlParser.lastMap).isEmpty();
    }

    @Test
    void missingParserFailsWithCapabilityName() {
        TemplateParser parser = new TemplateParser(null, null, null);

        assertThatThrownBy(() -> parser.parseWordByData(new byte[]{1}, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Word template parser is not available");
        assertThatThrownBy(() -> parser.parseExcelByData(new byte[]{1}, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Excel template parser is not available");
        assertThatThrownBy(() -> parser.parseHtmlByData(new byte[]{1}, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("HTML template parser is not available");
    }

    @Test
    void invalidInputFailsBeforeMissingParserCapability() {
        TemplateParser parser = new TemplateParser(null, null, null);

        assertThatThrownBy(() -> parser.parseWordByData(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Word template data")
                .hasMessageContaining("must not be empty");
        assertThatThrownBy(() -> parser.parseExcelByData(new byte[0], null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Excel template data")
                .hasMessageContaining("must not be empty");
        assertThatThrownBy(() -> parser.parseHtmlByData(new byte[0], null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTML template data")
                .hasMessageContaining("must not be empty");

        assertThatThrownBy(() -> parser.parseWordByUrl(" ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Word template URL")
                .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> parser.parseExcelByUrl(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Excel template URL")
                .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> parser.parseHtmlByUrl("", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTML template URL")
                .hasMessageContaining("must not be blank");
    }

    @Test
    void invalidDataFailsBeforeDelegating() {
        RecordingWordParser wordParser = new RecordingWordParser();
        TemplateParser parser = new TemplateParser(wordParser, new RecordingExcelParser(), new RecordingHtmlParser());

        assertThatThrownBy(() -> parser.parseWordByData(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Word template data")
                .hasMessageContaining("must not be empty");
        assertThatThrownBy(() -> parser.parseExcelByData(new byte[0], null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Excel template data")
                .hasMessageContaining("must not be empty");
        assertThatThrownBy(() -> parser.parseHtmlByData(new byte[0], null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTML template data")
                .hasMessageContaining("must not be empty");
        assertThat(wordParser.parseByDataCalled).isFalse();
    }

    @Test
    void invalidUrlFailsBeforeDelegating() {
        RecordingWordParser wordParser = new RecordingWordParser();
        TemplateParser parser = new TemplateParser(wordParser, new RecordingExcelParser(), new RecordingHtmlParser());

        assertThatThrownBy(() -> parser.parseWordByUrl(" ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Word template URL")
                .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> parser.parseExcelByUrl(null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Excel template URL")
                .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> parser.parseHtmlByUrl("", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTML template URL")
                .hasMessageContaining("must not be blank");
        assertThat(wordParser.parseByUrlCalled).isFalse();
    }

    @Test
    void parserExceptionKeepsRuntimeExceptionWrapperContextAndCauseForAllEntries() {
        RuntimeException root = new RuntimeException("broken docx");
        TemplateParser parser = new TemplateParser(new FailingWordParser(root),
                new FailingExcelParser(root), new FailingHtmlParser(root));

        assertParserFailure(() -> parser.parseWordByData(new byte[]{1}, null),
                "Failed to parse Word template from data", root);
        assertParserFailure(() -> parser.parseWordByUrl("https://example.com/template.docx", null),
                "Failed to parse Word template from URL", root);
        assertParserFailure(() -> parser.parseExcelByData(new byte[]{1}, null),
                "Failed to parse Excel template from data", root);
        assertParserFailure(() -> parser.parseExcelByUrl("https://example.com/template.xlsx", null),
                "Failed to parse Excel template from URL", root);
        assertParserFailure(() -> parser.parseHtmlByData(new byte[]{1}, null),
                "Failed to parse HTML template from data", root);
        assertParserFailure(() -> parser.parseHtmlByUrl("https://example.com/template.html", null),
                "Failed to parse HTML template from URL", root);
    }

    private static void assertParserFailure(ThrowingRunnable runnable, String message, RuntimeException root) {
        assertThatThrownBy(runnable::run)
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage(message)
                .hasCause(root);
    }

    private static class RecordingWordParser extends WordAbstractParser {
        private static final byte[] DATA_RESULT = new byte[]{11};
        private static final byte[] URL_RESULT = new byte[]{12};
        private boolean parseByDataCalled;
        private boolean parseByUrlCalled;
        private String lastUrl;
        private Map<String, Object> lastMap;

        @Override
        public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) {
            return parseByData(data, map);
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            this.parseByDataCalled = true;
            this.lastMap = map;
            return DATA_RESULT;
        }

        @Override
        public byte[] parseByUrl(String url, Map<String, Object> map) {
            this.parseByUrlCalled = true;
            this.lastUrl = url;
            this.lastMap = map;
            return URL_RESULT;
        }

        @Override
        public byte[] toHtml(byte[] srcBytes) {
            return new byte[0];
        }

        @Override
        public byte[] toPdf(byte[] srcBytes) {
            return new byte[0];
        }
    }

    private static class RecordingExcelParser extends ExcelAbstractParser {
        private static final byte[] DATA_RESULT = new byte[]{21};
        private Map<String, Object> lastMap;

        @Override
        public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) {
            return parseByData(data, map);
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            this.lastMap = map;
            return DATA_RESULT;
        }

        @Override
        public byte[] toHtml(byte[] srcBytes) {
            return new byte[0];
        }

        @Override
        public byte[] toPdf(byte[] srcBytes) {
            return new byte[0];
        }
    }

    private static class RecordingHtmlParser extends HtmlAbstractParser {
        private static final byte[] DATA_RESULT = new byte[]{31};
        private Map<String, Object> lastMap;

        @Override
        public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) {
            return parseByData(data, map);
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            this.lastMap = map;
            return DATA_RESULT;
        }

        @Override
        public byte[] toHtml(byte[] srcBytes) {
            return new byte[0];
        }

        @Override
        public byte[] toPdf(byte[] srcBytes) {
            return new byte[0];
        }
    }

    private static final class FailingWordParser extends RecordingWordParser {
        private final RuntimeException failure;

        private FailingWordParser(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            throw failure;
        }

        @Override
        public byte[] parseByUrl(String url, Map<String, Object> map) {
            throw failure;
        }
    }

    private static final class FailingExcelParser extends RecordingExcelParser {
        private final RuntimeException failure;

        private FailingExcelParser(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            throw failure;
        }

        @Override
        public byte[] parseByUrl(String url, Map<String, Object> map) {
            throw failure;
        }
    }

    private static final class FailingHtmlParser extends RecordingHtmlParser {
        private final RuntimeException failure;

        private FailingHtmlParser(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            throw failure;
        }

        @Override
        public byte[] parseByUrl(String url, Map<String, Object> map) {
            throw failure;
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
