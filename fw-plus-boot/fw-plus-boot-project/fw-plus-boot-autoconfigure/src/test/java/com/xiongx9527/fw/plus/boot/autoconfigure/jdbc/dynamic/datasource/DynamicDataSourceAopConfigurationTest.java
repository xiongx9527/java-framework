package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource;

import com.baomidou.dynamic.datasource.processor.DsProcessor;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.springframework.aop.Advisor;

import static org.junit.jupiter.api.Assertions.*;

class DynamicDataSourceAopConfigurationTest {

    @Test
    void dynamicDatasourceAnnotationAdvisorUsesDefaultAopPropertiesWhenMissing() {
        DynamicDataSourceProperties properties = new DynamicDataSourceProperties();
        properties.setAop(null);
        DynamicDataSourceAopConfiguration configuration = new DynamicDataSourceAopConfiguration(properties);

        Advisor advisor = configuration.dynamicDatasourceAnnotationAdvisor(new NoopDsProcessor());

        assertNotNull(advisor);
        assertNotNull(advisor.getAdvice());
    }

    @Test
    void dynamicTransactionAdvisorUsesDefaultAopPropertiesWhenMissing() {
        DynamicDataSourceProperties properties = new DynamicDataSourceProperties();
        properties.setAop(null);
        DynamicDataSourceAopConfiguration configuration = new DynamicDataSourceAopConfiguration(properties);

        Advisor advisor = configuration.dynamicTransactionAdvisor();

        assertNotNull(advisor);
        Advice advice = advisor.getAdvice();
        assertNotNull(advice);
    }

    private static class NoopDsProcessor extends DsProcessor {
        @Override
        public boolean matches(String key) {
            return false;
        }

        @Override
        public String doDetermineDatasource(MethodInvocation invocation, String key) {
            return null;
        }
    }
}
