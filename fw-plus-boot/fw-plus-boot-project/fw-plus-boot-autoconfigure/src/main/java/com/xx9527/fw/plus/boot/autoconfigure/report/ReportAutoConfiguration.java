package com.xx9527.fw.plus.boot.autoconfigure.report;

import com.xx9527.fw.plus.boot.autoconfigure.report.webmvc.ReportWebMvcAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
@ConditionalOnWebApplication
@ConditionalOnProperty(prefix = "fw.plus.report", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@Import({ReportWebMvcAutoConfiguration.class})
public class ReportAutoConfiguration {
}
