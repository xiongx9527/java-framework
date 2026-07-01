package com.xx9527.fw.plus.boot.autoconfigure.mate.webmvc;

import com.xx9527.fw.plus.boot.autoconfigure.web.WebAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

//@ConditionalOnExpression("#{T(com.xx9527.fw.plus.common.util.SpringUtils).isWebMvc()}")
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
@Configuration
@AutoConfigureAfter(WebAutoConfiguration.class)
@Import({MateWebMvcValidatorAutoConfiguration.class})
public class MateWebMvcAutoConfiguration {
}
