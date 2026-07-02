package com.xiongx9527.fw.plus.boot.autoconfigure.template;

import com.xiongx9527.fw.plus.template.TemplateParser;
import com.xiongx9527.fw.plus.template.excel.ExcelParser;
import com.xiongx9527.fw.plus.template.html.HtmlParser;
import com.xiongx9527.fw.plus.template.pdf.PdfParser;
import com.xiongx9527.fw.plus.template.word.WordParser;
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
    @ConditionalOnMissingClass
    public WordParser wordParser() {
        return new WordParser();
    }

    /**
     * 创建Excel文档解析器bean实例
     * 当容器中不存在ExcelParser类时才会创建该bean
     *
     * @return ExcelParser Excel文档解析器实例
     */
    @Bean
    @ConditionalOnMissingClass
    public ExcelParser excelParser () {
        return new ExcelParser();
    }

    /**
     * 创建HTML文档解析器bean实例
     * 当容器中不存在HtmlParser类时才会创建该bean
     *
     * @return HtmlParser HTML文档解析器实例
     */
    @Bean
    @ConditionalOnMissingClass
    public HtmlParser htmlParser () {
        return new HtmlParser();
    }

    /**
     * 创建PDF文档解析器bean实例
     * 当容器中不存在PdfParser类时才会创建该bean
     *
     * @return PdfParser PDF文档解析器实例
     */
    @Bean
    @ConditionalOnMissingClass
    public PdfParser pdfParser() {
        return new PdfParser();
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
     * @param pdfParser   PDF文档解析器
     * @return TemplateParser模板解析器实例
     */
    @Bean
    @ConditionalOnMissingClass
    public TemplateParser templateParser(WordParser wordParser, ExcelParser excelParser, HtmlParser htmlParser, PdfParser pdfParser) {
        return new TemplateParser(wordParser, excelParser, htmlParser, pdfParser);
    }

}
