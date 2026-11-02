package com.xiongx9527.fw.plus.boot.autoconfigure.debounce;

import com.xiongx9527.fw.plus.common.debounce.DebounceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 防抖配置属性装配：与 {@link DebounceAutoConfiguration} 分离，保证
 * {@code fw.plus.debounce.enabled=false}（缺省）时虽然不装配防抖组件，
 * 业务侧仍能注入 {@link DebounceProperties}（例如异常处理器需要默认窗口）。
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DebounceProperties.class)
public class DebouncePropertiesConfiguration {
}
