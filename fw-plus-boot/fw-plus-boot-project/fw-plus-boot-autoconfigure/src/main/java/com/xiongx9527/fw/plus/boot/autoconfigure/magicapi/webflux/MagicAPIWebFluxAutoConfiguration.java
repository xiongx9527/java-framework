package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.MagicAPIManagementService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.webflux.MagicAPIWebFluxController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options.MagicJdbcClassOptionProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.options.MagicJdbcClassOptionProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service.DataSourceBuilder;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service.MyDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider.ResultProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.resource.MyDefaultMagicResourceService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.ssssssss.magicapi.core.config.Constants;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.config.ResponseCode;
import org.ssssssss.magicapi.core.config.Security;
import org.ssssssss.magicapi.core.interceptor.AuthorizationInterceptor;
import org.ssssssss.magicapi.core.interceptor.DefaultAuthorizationInterceptor;
import org.ssssssss.magicapi.core.interceptor.RequestInterceptor;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.resource.ResourceAdapter;
import org.ssssssss.magicapi.core.service.MagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.core.service.impl.ApiInfoMagicResourceStorage;
import org.ssssssss.magicapi.core.web.RequestHandler;
import org.ssssssss.magicapi.datasource.model.DataSourceInfo;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;
import org.ssssssss.magicapi.datasource.service.DataSourceMagicDynamicRegistry;
import org.ssssssss.magicapi.datasource.service.DataSourceInfoMagicResourceStorage;
import org.ssssssss.magicapi.function.service.FunctionInfoMagicResourceStorage;
import org.ssssssss.magicapi.function.service.FunctionMagicDynamicRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(prefix = "fw.plus.magicapi", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")
@EnableConfigurationProperties({MagicAPIProperties.class, MagicJdbcClassOptionProperties.class})
public class MagicAPIWebFluxAutoConfiguration implements WebFluxConfigurer {

    private final MagicAPIProperties properties;

    public MagicAPIWebFluxAutoConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }

    @Override
    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
        configurer.addCustomResolver(new WebFluxMagicAPIArgumentResolver());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String web = properties.getWeb();
        if (web != null) {
            org.ssssssss.magicapi.core.logging.LoggerManager.createMagicAppender();
            registry.addResourceHandler(web + "/**").addResourceLocations("classpath:/magic-editor/");
        }
    }

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
    public FunctionMagicDynamicRegistry functionMagicDynamicRegistry(
            FunctionInfoMagicResourceStorage functionInfoMagicResourceStorage) {
        return new FunctionMagicDynamicRegistry(functionInfoMagicResourceStorage);
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicDataSource magicDynamicDataSource() {
        return new MagicDynamicDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public DataSourceMagicDynamicRegistry dataSourceMagicDynamicRegistry(
            DataSourceInfoMagicResourceStorage magicResourceStorage,
            MagicDynamicDataSource magicDynamicDataSource,
            Map<String, DataSourceBuilder> dataSourceBuilderMap) {
        for (String className : MyDataSourceMagicDynamicRegistry.DATA_SOURCE_TYPE_NAMES) {
            try {
                Class.forName(className);
                return new MyDataSourceMagicDynamicRegistry(magicResourceStorage, magicDynamicDataSource,
                        dataSourceBuilderMap);
            } catch (ClassNotFoundException ignored) {
            }
        }
        return new EmptyDataSourceMagicDynamicRegistry(magicResourceStorage, magicDynamicDataSource);
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
                                                  org.ssssssss.magicapi.core.resource.Resource workspace,
                                                  org.ssssssss.magicapi.core.interceptor.ResultProvider resultProvider,
                                                  ObjectProvider<List<HttpMessageConverter<?>>> httpMessageConvertersProvider,
                                                  ObjectProvider<List<RequestInterceptor>> requestInterceptorsProvider,
                                                  ObjectProvider<List<MagicDynamicRegistry<? extends MagicEntity>>> magicDynamicRegistriesProvider) {
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
        configuration.setResultProvider(resultProvider);
        configuration.setHttpMessageConverters(httpMessageConvertersProvider.getIfAvailable(Collections::emptyList));
        configuration.setMagicDynamicRegistries(magicDynamicRegistriesProvider.getIfAvailable(Collections::emptyList));
        configuration.setEnableWeb(properties.getWeb() != null);
        requestInterceptorsProvider.getIfAvailable(Collections::emptyList).forEach(configuration::addRequestInterceptor);
        return configuration;
    }

    @Bean
    @ConditionalOnMissingBean
    public org.ssssssss.magicapi.core.interceptor.ResultProvider resultProvider(MagicAPIProperties properties) {
        return new ResultProvider(properties);
    }

    @Bean
    @ConditionalOnBean(RequestMappingHandlerMapping.class)
    @ConditionalOnMissingBean
    public MagicAPIWebFluxMapping magicAPIWebFluxMapping(RequestMappingHandlerMapping requestMappingHandlerMapping,
                                                         MagicAPIProperties properties,
                                                         ApplicationContext applicationContext) {
        if (applicationContext instanceof ConfigurableApplicationContext) {
            return new MagicAPIWebFluxMapping(requestMappingHandlerMapping, properties.getWeb(),
                    ((ConfigurableApplicationContext) applicationContext).getBeanFactory()::resolveEmbeddedValue);
        }
        return new MagicAPIWebFluxMapping(requestMappingHandlerMapping, properties.getWeb());
    }

    @Bean
    @ConditionalOnBean(MagicAPIWebFluxMapping.class)
    @ConditionalOnMissingBean
    public WebFluxRequestMagicDynamicRegistry webFluxRequestMagicDynamicRegistry(
            ApiInfoMagicResourceStorage apiInfoMagicResourceStorage,
            MagicAPIWebFluxMapping mapping,
            MagicAPIProperties properties) throws NoSuchMethodException {
        return new WebFluxRequestMagicDynamicRegistry(apiInfoMagicResourceStorage, mapping,
                properties.isAllowOverride(), properties.getPrefix());
    }

    @Bean
    @ConditionalOnBean(WebFluxRequestMagicDynamicRegistry.class)
    @ConditionalOnMissingBean
    public RequestHandler requestHandler(MagicConfiguration configuration,
                                         WebFluxRequestMagicDynamicRegistry registry) {
        RequestHandler requestHandler = new RequestHandler(configuration, registry);
        registry.setHandler(new MagicAPIWebFluxRequestHandler(requestHandler));
        return requestHandler;
    }

    @Bean
    @ConditionalOnBean(MagicAPIWebFluxMapping.class)
    @ConditionalOnMissingBean
    public MagicAPIWebFluxControllerRegistrar magicAPIWebFluxControllerRegistrar(
            MagicAPIWebFluxMapping mapping,
            MagicConfiguration configuration,
            MagicAPIProperties properties,
            ObjectProvider<List<MagicPluginConfiguration>> magicPluginConfigurationsProvider,
            ApplicationContext applicationContext) {
        return new MagicAPIWebFluxControllerRegistrar(mapping, configuration, properties,
                magicPluginConfigurationsProvider.getIfAvailable(Collections::emptyList), applicationContext);
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

    @Bean
    @ConditionalOnMissingBean
    public MagicAPIWebFluxExceptionHandler magicAPIWebFluxExceptionHandler() {
        return new MagicAPIWebFluxExceptionHandler();
    }

    static class EmptyDataSourceMagicDynamicRegistry extends DataSourceMagicDynamicRegistry {

        EmptyDataSourceMagicDynamicRegistry(MagicResourceStorage<DataSourceInfo> magicResourceStorage,
                                            MagicDynamicDataSource magicDynamicDataSource) {
            super(magicResourceStorage, magicDynamicDataSource);
        }

        @Override
        public List<DataSourceInfo> defaultMappings() {
            return new ArrayList<>();
        }
    }
}
