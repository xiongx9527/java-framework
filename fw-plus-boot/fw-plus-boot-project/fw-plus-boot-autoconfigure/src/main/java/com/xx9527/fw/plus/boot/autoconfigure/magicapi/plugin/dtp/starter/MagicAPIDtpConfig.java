package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.dtp.starter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;

import java.util.HashMap;

/**
 * MagicAPI动态线程池配置类
 *
 * <p>该类用于配置MagicAPI插件的动态线程池相关属性，支持配置文件中的属性绑定和动态刷新功能。
 * 配置属性前缀为"magic-api.plugin.dtp"，可以通过配置文件进行自定义配置。</p>
 *
 * <p>该类继承自HashMap，允许存储任意类型的配置键值对，提供了灵活的配置扩展能力。</p>
 *
 * @author [作者名]
 * @since [版本号]
 */
@RefreshScope
@ConfigurationProperties("magic-api.plugin.dtp")
public class MagicAPIDtpConfig extends HashMap<String, Object> {
}

