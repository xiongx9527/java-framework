package com.xx9527.fw.plus.boot.autoconfigure;

import com.xx9527.fw.plus.common.util.SpringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : jugui
 */
//@ConditionalOnWebApplication
@Configuration
public class BaseAutoConfiguration {



    @Bean
    public SpringUtils springUtils() {
        return new SpringUtils();
    }
}
