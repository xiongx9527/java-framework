package com.xx9527.fw.plus.boot.autoconfigure.magicapi.modules.template;

import com.xx9527.fw.plus.template.TemplateParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name={"com.xx9527.fw.plus.template.TemplateParser"})
public class TemplateModuleConfiguration {

    /**
     * 创建并配置模板模块的Bean实例
     *
     * @return TemplateModule 模板模块实例
     */
    @Bean
    public TemplateModule templateModule(@Autowired(required = false) TemplateParser templateParser) {
        if (templateParser != null) {
            return new TemplateModule(templateParser);
        }
        return new TemplateModule();
    }

}
