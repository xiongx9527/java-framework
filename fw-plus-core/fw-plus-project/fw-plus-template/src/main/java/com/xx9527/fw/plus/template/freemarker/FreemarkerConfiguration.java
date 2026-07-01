package com.xx9527.fw.plus.template.freemarker;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.TemplateClassResolver;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

public class FreemarkerConfiguration extends Configuration {

    public FreemarkerConfiguration() {
        super(Configuration.VERSION_2_3_28);

        TemplateLoader[] templateLoaders = {new FreemarkerTemplateLoader()};
        super.setTemplateLoader(new MultiTemplateLoader(templateLoaders));

        super.setDefaultEncoding("UTF-8");
        super.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        super.setLogTemplateExceptions(false);
        super.setWrapUncheckedExceptions(true);
        super.setNewBuiltinClassResolver(TemplateClassResolver.SAFER_RESOLVER);
    }

}