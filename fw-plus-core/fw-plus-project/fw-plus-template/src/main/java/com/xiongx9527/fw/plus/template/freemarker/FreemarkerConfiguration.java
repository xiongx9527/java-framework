package com.xiongx9527.fw.plus.template.freemarker;

import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

/**
 * Freemarker配置类
 *
 * <p>该类继承自Freemarker的Configuration类，用于初始化和配置Freemarker模板引擎的相关设置。
 * 主要包括模板加载器、编码格式、异常处理策略等配置项。</p>
 */
public class FreemarkerConfiguration extends Configuration {

    /**
     * 构造函数
     *
     * <p>初始化Freemarker配置，设置模板版本、模板加载器、编码格式和异常处理策略等参数。</p>
     */
    public FreemarkerConfiguration() {
        super(Configuration.VERSION_2_3_28);

        // 配置模板加载器
        TemplateLoader[] templateLoaders = {new FreemarkerTemplateLoader()};
        super.setTemplateLoader(new MultiTemplateLoader(templateLoaders));

        // 设置默认编码格式和异常处理策略
        super.setDefaultEncoding("UTF-8");
        super.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        super.setLogTemplateExceptions(false);
        super.setWrapUncheckedExceptions(true);
    }

}

