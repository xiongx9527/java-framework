package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.runtime;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaEntityDiagnostic;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.springbean.model.SpringBeanInfo;
import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.aop.Advisor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.support.DefaultPointcutAdvisor;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicSpringBeanRegistryTest {

    private AnnotationConfigApplicationContext context;

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void registersTransactionalBeanThroughSpringBeanDefinitionPipeline() throws Exception {
        context = new AnnotationConfigApplicationContext(TransactionTestConfiguration.class);
        CountingTransactionManager transactionManager = context.getBean(CountingTransactionManager.class);
        DynamicSpringBeanRegistry registry = registry(context);

        assertTrue(registry.rebuild(Collections.singletonList(bean("tx", "TxService",
                "package demo;\n"
                        + "import org.springframework.transaction.annotation.Transactional;\n"
                        + "public class TxService {\n"
                        + "  @Transactional\n"
                        + "  public String work() { return \"ok\"; }\n"
                        + "}\n"))), diagnostics(registry));

        Object bean = context.getBean("txService");
        assertTrue(AopUtils.isAopProxy(bean));
        assertEquals("ok", bean.getClass().getMethod("work").invoke(bean));
        assertEquals(1, transactionManager.beginCount);
        assertEquals(1, transactionManager.commitCount);
    }

    @Test
    void failedUpdateKeepsLastSuccessfulBeanDefinition() throws Exception {
        context = new AnnotationConfigApplicationContext(TransactionTestConfiguration.class);
        DynamicSpringBeanRegistry registry = registry(context);

        assertTrue(registry.rebuild(Collections.singletonList(bean("svc", "DemoService",
                "package demo;\n"
                        + "public class DemoService {\n"
                        + "  public String version() { return \"v1\"; }\n"
                        + "}\n"))));
        Object first = context.getBean("demoService");
        Method version = first.getClass().getMethod("version");
        assertEquals("v1", version.invoke(first));

        assertFalse(registry.rebuild(Collections.singletonList(bean("svc", "DemoService",
                "package demo;\n"
                        + "public class DemoService {\n"
                        + "  private MissingType value;\n"
                        + "}\n"))));

        Object current = context.getBean("demoService");
        assertEquals("v1", current.getClass().getMethod("version").invoke(current));
    }

    @Test
    void supportsLombokAnnotationsWhenCompilingSpringBeanSource() throws Exception {
        context = new AnnotationConfigApplicationContext(TransactionTestConfiguration.class);
        DynamicSpringBeanRegistry registry = registry(context);

        assertTrue(registry.rebuild(Collections.singletonList(bean("lombok", "LombokService",
                "package demo;\n"
                        + "import lombok.Data;\n"
                        + "import lombok.NoArgsConstructor;\n"
                        + "@Data\n"
                        + "@NoArgsConstructor\n"
                        + "public class LombokService {\n"
                        + "  private String name;\n"
                        + "}\n"))), diagnostics(registry));

        Object bean = context.getBean("lombokService");
        bean.getClass().getMethod("setName", String.class).invoke(bean, "张三");
        assertEquals("张三", bean.getClass().getMethod("getName").invoke(bean));
    }

    @Test
    void supportsDynamicDatasourceAnnotationThroughSpringAopPipeline() throws Exception {
        context = new AnnotationConfigApplicationContext(DsTestConfiguration.class);
        CountingDsInterceptor interceptor = context.getBean(CountingDsInterceptor.class);
        DynamicSpringBeanRegistry registry = registry(context);

        assertTrue(registry.rebuild(Collections.singletonList(bean("ds", "DsService",
                "package demo;\n"
                        + "import com.baomidou.dynamic.datasource.annotation.DS;\n"
                        + "public class DsService {\n"
                        + "  @DS(\"slave\")\n"
                        + "  public String work() { return \"ok\"; }\n"
                        + "}\n"))), diagnostics(registry));

        Object bean = context.getBean("dsService");
        assertTrue(AopUtils.isAopProxy(bean));
        assertEquals("ok", bean.getClass().getMethod("work").invoke(bean));
        assertEquals(1, interceptor.invokeCount);
    }

    private static DynamicSpringBeanRegistry registry(AnnotationConfigApplicationContext context) {
        return new DynamicSpringBeanRegistry(context, new JavaSourceAnalyzer(), new InMemoryJavaEntityCompiler());
    }

    private static SpringBeanInfo bean(String id, String name, String source) {
        SpringBeanInfo info = new SpringBeanInfo();
        info.setId(id);
        info.setName(name);
        info.setScript(source);
        return info;
    }

    private static String diagnostics(DynamicSpringBeanRegistry registry) {
        StringBuilder builder = new StringBuilder();
        for (JavaEntityDiagnostic diagnostic : registry.status().getDiagnostics()) {
            builder.append(diagnostic.getCode()).append(": ").append(diagnostic.getMessage()).append('\n');
        }
        return builder.toString();
    }

    @Configuration
    @EnableTransactionManagement
    static class TransactionTestConfiguration {

        @Bean
        CountingTransactionManager transactionManager() {
            return new CountingTransactionManager();
        }
    }

    static class CountingTransactionManager extends AbstractPlatformTransactionManager
            implements PlatformTransactionManager {

        int beginCount;
        int commitCount;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            beginCount++;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commitCount++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @EnableTransactionManagement
    static class DsTestConfiguration {

        @Bean
        CountingDsInterceptor countingDsInterceptor() {
            return new CountingDsInterceptor();
        }

        @Bean
        Advisor dsAdvisor(CountingDsInterceptor interceptor) {
            return new DefaultPointcutAdvisor(new AnnotationMatchingPointcut(null, DS.class), interceptor);
        }
    }

    static class CountingDsInterceptor implements MethodInterceptor {

        int invokeCount;

        @Override
        public Object invoke(org.aopalliance.intercept.MethodInvocation invocation) throws Throwable {
            invokeCount++;
            return invocation.proceed();
        }
    }
}
