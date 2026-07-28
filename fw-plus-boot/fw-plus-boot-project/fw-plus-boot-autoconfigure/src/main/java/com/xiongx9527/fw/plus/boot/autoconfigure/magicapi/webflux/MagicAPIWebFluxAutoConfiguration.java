package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.MagicAPIManagementService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.webflux.MagicAPIWebFluxController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options.MagicJdbcClassOptionProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options.MagicJdbcClassOptionProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service.DataSourceBuilder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.resource.MyDefaultMagicResourceService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.config.ResponseCode;
import org.ssssssss.magicapi.core.config.Security;
import org.ssssssss.magicapi.core.interceptor.AuthorizationInterceptor;
import org.ssssssss.magicapi.core.interceptor.DefaultAuthorizationInterceptor;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.ResourceAdapter;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.core.service.impl.ApiInfoMagicResourceStorage;
import org.ssssssss.magicapi.datasource.service.DataSourceInfoMagicResourceStorage;
import org.ssssssss.magicapi.function.service.FunctionInfoMagicResourceStorage;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "fw.plus.magicapi", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")
@EnableConfigurationProperties({MagicAPIProperties.class, MagicJdbcClassOptionProperties.class})
public class MagicAPIWebFluxAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationInterceptor authorizationInterceptor(MagicAPIProperties properties) {
        Security security = properties.getSecurity();
        return new DefaultAuthorizationInterceptor(security.getUsername(), security.getPassword());
    }

    @Bean
    @ConditionalOnMissingBean(org.ssssssss.magicapi.core.resource.Resource.class)
    @ConditionalOnProperty(prefix = "magic-api", name = "resource.type", havingValue = "file", matchIfMissing = true)
    public org.ssssssss.magicapi.core.resource.Resource magicWebFluxResource(MagicAPIProperties properties) throws IOException {
        org.ssssssss.magicapi.core.config.Resource resource = properties.getResource();
        return ResourceAdapter.getResource(resource.getLocation(), resource.isReadonly());
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiInfoMagicResourceStorage apiInfoMagicResourceStorage(MagicAPIProperties properties) {
        return new ApiInfoMagicResourceStorage(properties.getPrefix());
    }

    @Bean
    @ConditionalOnMissingBean
    public FunctionInfoMagicResourceStorage functionInfoMagicResourceStorage() {
        return new FunctionInfoMagicResourceStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSourceInfoMagicResourceStorage dataSourceInfoMagicResourceStorage() {
        return new DataSourceInfoMagicResourceStorage();
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicResourceService magicResourceService(
            org.ssssssss.magicapi.core.resource.Resource workspace,
            ObjectProvider<List<MagicResourceStorage<? extends MagicEntity>>> magicResourceStoragesProvider,
            ApplicationContext applicationContext) {
        return new MyDefaultMagicResourceService(workspace,
                magicResourceStoragesProvider.getIfAvailable(Collections::emptyList),
                applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicConfiguration magicConfiguration(MagicAPIProperties properties,
                                                  AuthorizationInterceptor authorizationInterceptor,
                                                  MagicResourceService magicResourceService,
                                                  org.ssssssss.magicapi.core.resource.Resource workspace) {
        ResponseCode responseCodeConfig = properties.getResponseCode();
        Constants.RESPONSE_CODE_SUCCESS = responseCodeConfig.getSuccess();
        Constants.RESPONSE_CODE_INVALID = responseCodeConfig.getInvalid();
        Constants.RESPONSE_CODE_EXCEPTION = responseCodeConfig.getException();

        MagicConfiguration configuration = new MagicConfiguration();
        configuration.setAuthorizationInterceptor(authorizationInterceptor);
        configuration.setMagicResourceService(magicResourceService);
        configuration.setInstanceId(properties.getInstanceId());
        configuration.setDebugTimeout(properties.getDebug().getTimeout());
        configuration.setThrowException(properties.isThrowException());
        configuration.setEditorConfig(properties.getEditorConfig());
        configuration.setWorkspace(workspace);
        configuration.setMagicDynamicRegistries(Collections.emptyList());
        configuration.setEnableWeb(properties.getWeb() != null);
        return configuration;
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicJdbcClassOptionProvider magicJdbcClassOptionProvider(MagicJdbcClassOptionProperties properties,
                                                                      Map<String, DataSourceBuilder> dataSourceBuilderMap) {
        return new MagicJdbcClassOptionProvider(properties, dataSourceBuilderMap,
                MagicAPIWebFluxAutoConfiguration.class.getClassLoader());
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicAPIManagementService magicAPIManagementService(MagicConfiguration configuration,
                                                                 MagicJdbcClassOptionProvider optionProvider) {
        return new MagicAPIManagementService(configuration, optionProvider);
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicAPIWebFluxController magicAPIWebFluxController(MagicAPIManagementService managementService,
                                                                MagicConfiguration configuration) {
        return new MagicAPIWebFluxController(managementService, configuration);
    }
}
