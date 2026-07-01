package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.entity.SwaggerEntity;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
@Data
@ConfigurationProperties(prefix = "magic-api.swagger")
public class SwaggerConfig {

    private String name = "MagicAPI接口";

    /**
     * 资源位置
     */
    private String location = "/v2/api-docs/magic-api/swagger2.json";

    /**
     * 文档标题
     */
    private String title = "MagicAPI Swagger Docs";

    /**
     * 文档描述
     */
    private String description = "MagicAPI 接口信息";

    @NestedConfigurationProperty
    private SwaggerEntity.Concat concat = new SwaggerEntity.Concat();

    /**
     * 基本认证
     */
    @NestedConfigurationProperty
    private SwaggerEntity.BasicAuth basicAuth;

    /**
     * api密钥认证
     */
    @NestedConfigurationProperty
    private SwaggerEntity.ApiKeyAuth apiKeyAuth;

    /**
     * oauth2认证
     */
    @NestedConfigurationProperty
    private SwaggerEntity.OAuth2 oauth2;

    /**
     * 文档版本
     */
    private String version = "1.0";
}
