package com.xiongx9527.fw.plus.boot.autoconfigure.report.webmvc;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
@Configuration
@Import({JimuReportAutoConfiguration.class})
public class ReportWebMvcAutoConfiguration {
}
