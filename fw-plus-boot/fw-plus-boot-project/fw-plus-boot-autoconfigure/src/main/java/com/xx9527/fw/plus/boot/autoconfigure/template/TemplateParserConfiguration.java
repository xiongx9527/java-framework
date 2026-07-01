package com.xx9527.fw.plus.boot.autoconfigure.template;

import com.xx9527.fw.plus.template.TemplateParser;
import com.xx9527.fw.plus.template.excel.ExcelAbstractParser;
import com.xx9527.fw.plus.template.excel.ExcelParser;
import com.xx9527.fw.plus.template.filechange.*;
import com.xx9527.fw.plus.template.html.HtmlAbstractParser;
import com.xx9527.fw.plus.template.html.HtmlParser;
import com.xx9527.fw.plus.template.word.WordAbstractParser;
import com.xx9527.fw.plus.template.word.WordParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemplateParserConfiguration {

    /**
     * 创建Word文档解析器bean实例
     * 当容器中不存在WordParser类时才会创建该bean
     *
     * @return WordParser Word文档解析器实例
     */
    @Bean
    @ConditionalOnClass(name = "cn.afterturn.easypoi.word.parse.ParseWord07")
    @ConditionalOnMissingClass
    public WordAbstractParser wordParser(@Autowired(required = false) AbstractWord2Html word2Html,
                                         @Autowired(required = false) AbstractWord2Pdf word2Pdf) {
        return new WordParser(word2Html, word2Pdf);
    }

    /**
     * 创建Excel文档解析器bean实例
     * 当容器中不存在ExcelParser类时才会创建该bean
     *
     * @return ExcelParser Excel文档解析器实例
     */
    @Bean
    @ConditionalOnClass(name = "cn.afterturn.easypoi.excel.ExcelImportUtil")
    @ConditionalOnMissingClass
    public ExcelAbstractParser excelParser(@Autowired(required = false) AbstractExcel2Html excel2Html) {
        return new ExcelParser(excel2Html);
    }

    /**
     * 创建HTML文档解析器bean实例
     * 当容器中不存在HtmlParser类时才会创建该bean
     *
     * @return HtmlParser HTML文档解析器实例
     */
    @Bean
    @ConditionalOnClass(name = "freemarker.template.Template")
    @ConditionalOnMissingClass
    public HtmlAbstractParser htmlParser(@Autowired(required = false) AbstractHtml2Pdf html2Pdf) {
        return new HtmlParser(html2Pdf);
    }

    /**
     * 创建模板解析器Bean实例
     * <p>
     * 该方法用于创建TemplateParser对象，整合了多种文档格式的解析器。
     * 只有在缺少指定类时才会创建此Bean。
     *
     * @param wordParser  Word文档解析器
     * @param excelParser Excel文档解析器
     * @param htmlParser  HTML文档解析器
     * @return TemplateParser模板解析器实例
     */
    @Bean
    @ConditionalOnMissingClass
    public TemplateParser templateParser(@Autowired(required = false) WordAbstractParser wordParser,
                                         @Autowired(required = false) ExcelAbstractParser excelParser,
                                         @Autowired(required = false) HtmlAbstractParser htmlParser) {
        return new TemplateParser(wordParser, excelParser, htmlParser);
    }

}
