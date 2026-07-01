package com.xiongx9527.fw.plus.boot.autoconfigure.template;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(prefix = "fw.plus.template", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name={"com.xiongx9527.fw.plus.template.TemplateParser"})
@Import({TemplateParserConfiguration.class})
public class TemplateAutoConfiguration {

}
