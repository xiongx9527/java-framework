package com.xiongx9527.fw.plus.boot.autoconfigure.report.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.report.webmvc.jimu.JimuJsEnterpriseFilter;
import com.xiongx9527.fw.plus.boot.autoconfigure.report.webmvc.jimu.JimuReportSysSwitchFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Slf4j
@ConditionalOnClass(name = {
        "org.jeecg.modules.jmreport.config.init.JimuReportConfiguration",
        "org.jeecg.modules.jmreport.desreport.service.IJmreportNoSqlService"
})
@Configuration
@Import({JimuReportSysSwitchFilter.class, JimuJsEnterpriseFilter.class})
public class JimuReportAutoConfiguration  {

}
