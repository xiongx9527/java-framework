package com.xiongx9527.fw.plus.boot.autoconfigure.template;

import com.xiongx9527.fw.plus.template.TemplateParser;
import com.xiongx9527.fw.plus.template.excel.ExcelAbstractParser;
import com.xiongx9527.fw.plus.template.excel.ExcelParser;
import com.xiongx9527.fw.plus.template.html.HtmlAbstractParser;
import com.xiongx9527.fw.plus.template.html.HtmlParser;
import com.xiongx9527.fw.plus.template.word.WordAbstractParser;
import com.xiongx9527.fw.plus.template.word.WordParser;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TemplateParserConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TemplateParserConfiguration.class));

    @Test
    void createsDefaultParserBeansWhenNoUserBeansExist() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(WordAbstractParser.class);
            assertThat(context).hasSingleBean(ExcelAbstractParser.class);
            assertThat(context).hasSingleBean(HtmlAbstractParser.class);
            assertThat(context).hasSingleBean(TemplateParser.class);
            assertThat(context.getBean(WordAbstractParser.class)).isInstanceOf(WordParser.class);
            assertThat(context.getBean(ExcelAbstractParser.class)).isInstanceOf(ExcelParser.class);
            assertThat(context.getBean(HtmlAbstractParser.class)).isInstanceOf(HtmlParser.class);
        });
    }

    @Test
    void userParserBeansWinOverDefaults() {
        contextRunner.withUserConfiguration(UserParserConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(WordAbstractParser.class);
            assertThat(context).hasSingleBean(ExcelAbstractParser.class);
            assertThat(context).hasSingleBean(HtmlAbstractParser.class);
            assertThat(context.getBean(WordAbstractParser.class)).isInstanceOf(UserWordParser.class);
            assertThat(context.getBean(ExcelAbstractParser.class)).isInstanceOf(UserExcelParser.class);
            assertThat(context.getBean(HtmlAbstractParser.class)).isInstanceOf(UserHtmlParser.class);
        });
    }

    @Test
    void templateParserUsesBeansFromContext() {
        contextRunner.withUserConfiguration(UserParserConfiguration.class).run(context -> {
            TemplateParser parser = context.getBean(TemplateParser.class);
            assertThat(parser.getWordParser()).isSameAs(context.getBean(WordAbstractParser.class));
            assertThat(parser.getExcelParser()).isSameAs(context.getBean(ExcelAbstractParser.class));
            assertThat(parser.getHtmlParser()).isSameAs(context.getBean(HtmlAbstractParser.class));
        });
    }

    @Test
    void userTemplateParserWinsOverDefaultTemplateParser() {
        contextRunner.withUserConfiguration(UserTemplateParserConfiguration.class).run(context -> {
            assertThat(context).hasSingleBean(TemplateParser.class);
            assertThat(context.getBean(TemplateParser.class)).isSameAs(UserTemplateParserConfiguration.TEMPLATE_PARSER);
        });
    }

    @Test
    void missingWordClasspathSkipsDefaultWordParserButStillCreatesTemplateParser() {
        contextRunner.withClassLoader(new FilteredClassLoader("cn.afterturn.easypoi.word.parse.ParseWord07"))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(WordAbstractParser.class);
                    assertThat(context).hasSingleBean(TemplateParser.class);
                    assertThatThrownBy(() -> context.getBean(TemplateParser.class).parseWordByData(new byte[]{1}, null))
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("Word template parser is not available");
                });
    }

    @Configuration(proxyBeanMethods = false)
    static class UserParserConfiguration {
        @Bean
        WordAbstractParser wordAbstractParser() {
            return new UserWordParser();
        }

        @Bean
        ExcelAbstractParser excelAbstractParser() {
            return new UserExcelParser();
        }

        @Bean
        HtmlAbstractParser htmlAbstractParser() {
            return new UserHtmlParser();
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class UserTemplateParserConfiguration {
        private static final TemplateParser TEMPLATE_PARSER = new TemplateParser(null, null, null);

        @Bean
        TemplateParser templateParser() {
            return TEMPLATE_PARSER;
        }
    }

    static class UserWordParser extends WordAbstractParser {
        @Override
        public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) throws Exception {
            return parseByData(data, map);
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            return new byte[]{1};
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

    static class UserExcelParser extends ExcelAbstractParser {
        @Override
        public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) throws Exception {
            return parseByData(data, map);
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            return new byte[]{2};
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

    static class UserHtmlParser extends HtmlAbstractParser {
        @Override
        public byte[] parseByData(String beanName, byte[] data, Map<String, Object> map) throws Exception {
            return parseByData(data, map);
        }

        @Override
        public byte[] parseByData(byte[] data, Map<String, Object> map) {
            return new byte[]{3};
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
}
