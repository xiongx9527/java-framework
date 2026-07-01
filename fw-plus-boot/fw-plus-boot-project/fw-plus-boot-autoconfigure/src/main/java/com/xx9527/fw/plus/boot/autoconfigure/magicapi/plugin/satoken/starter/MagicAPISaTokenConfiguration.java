package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.satoken.starter;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.satoken.oauth2.granttype.handler.MagicApiSaOAuth2GrantTypeHandler;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.satoken.service.SaTokenMagicDynamicRegistry;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.satoken.service.SaTokenMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;

@ConditionalOnProperty(prefix = "magic-api.plugin.satoken", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name={"cn.dev33.satoken.oauth2.granttype.handler.SaOAuth2GrantTypeHandlerInterface",
        "cn.dev33.satoken.oauth2.data.model.AccessTokenModel","cn.dev33.satoken.context.model.SaRequest"})
@Configuration
public class MagicAPISaTokenConfiguration implements MagicPluginConfiguration {

    /**
     * 创建并配置SaToken插件实例
     *
     * @return SaToken插件对象，包含插件名称、显示名称和前端资源文件名
     */
    @Override
    public Plugin plugin() {
        return new Plugin("satoken", "MagicSatoken", "magic-satoken.1.0.0.iife.js");
    }

    /**
     * 创建SaToken Magic资源存储器Bean
     * 用于存储和管理SaToken相关的MagicAPI资源信息
     *
     * @return SaToken资源存储器实例
     */
    @Bean
    public SaTokenMagicResourceStorage saTokenMagicResourceStorage(){
        return new SaTokenMagicResourceStorage();
    }

    /**
     * 创建SaToken Magic动态注册器Bean
     * 负责动态注册和管理SaToken相关的MagicAPI服务
     *
     * @param saTokenMagicResourceStorage SaToken资源存储器，用于提供资源管理服务
     * @return SaToken动态注册器实例
     */
    @Bean
    public SaTokenMagicDynamicRegistry saTokenMagicDynamicRegistry(SaTokenMagicResourceStorage saTokenMagicResourceStorage){
        return new SaTokenMagicDynamicRegistry(saTokenMagicResourceStorage);
    }

    /**
     * 创建MagicAPI SaToken OAuth2授权类型处理器Bean
     * 处理MagicAPI与SaToken集成时的OAuth2授权逻辑
     *
     * @param saTokenMagicResourceStorage SaToken资源存储器，用于提供资源管理服务
     * @return OAuth2授权类型处理器实例
     */
    @Bean
    public MagicApiSaOAuth2GrantTypeHandler magicApiSaOAuth2GrantTypeHandler(SaTokenMagicResourceStorage saTokenMagicResourceStorage){
        return new MagicApiSaOAuth2GrantTypeHandler(saTokenMagicResourceStorage);
    }

}
