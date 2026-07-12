package com.xiongx9527.fw.plus.boot.autoconfigure.config.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "fw.plus.config.client")
public class ConfigClientProperties {

    private boolean enabled = true;

    private boolean refreshEnabled = true;

    private int refreshInterval = 60;

    private boolean cacheEnabled = true;

    private String cacheDir;

    private int retryCount = 3;

    public int getRefreshInterval() {
        return Math.max(refreshInterval, 5);
    }
}
