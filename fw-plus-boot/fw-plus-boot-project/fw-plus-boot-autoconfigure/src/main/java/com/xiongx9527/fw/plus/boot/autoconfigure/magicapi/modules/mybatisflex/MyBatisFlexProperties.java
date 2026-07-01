package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.mybatisflex;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(
        prefix = "magic.api.mybatis-flex"
)
public class MyBatisFlexProperties {
    private boolean mapUnderscoreToCamelCase;
}
