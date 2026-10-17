package com.xiongx9527.fw.plus.boot.mate.sample;

import org.dromara.core.trans.vo.VO;
import org.dromara.trans.service.impl.SimpleTransService;
//import com.github.kristofa.brave.spring.BraveClientHttpRequestInterceptor;
//import org.springframework.cloud.sleuth.instrument.web.client.feign.FeignContextBeanPostProcessor;
import com.xiongx9527.fw.plus.common.databind.ISensitiveStrategy;
import com.xiongx9527.fw.plus.boot.mate.sample.dto.vo.UserExtInfo;
import com.xiongx9527.fw.plus.common.strategy.SensitiveStrategy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;


//@EnableDiscoveryClient
@SpringBootApplication
public class MateSampleApplication {

    public static void main(String[] args) {

        //BraveClientHttpRequestInterceptor dd;
        //TracingFeignClientAutoConfiguration ffff;

        SpringApplication.run(MateSampleApplication.class, args);
    }


    //2025-01-04 21:40:15.473  WARN 12245 --- [nio-8080-exec-1] .w.s.m.s.DefaultHandlerExceptionResolver : Resolved [org.springframework.web.bind.MethodArgumentNotValidException: Validation failed for argument [0] in public com.xiongx9527.fw.plus.boot.mate.sample.dto.vo.UserInfoVo com.xiongx9527.fw.plus.boot.mate.sample.controller.TestController.saveUser(com.xiongx9527.fw.plus.boot.mate.sample.dto.form.SaveUserForm): [Field error in object 'saveUserForm' on field 'userExtId': rejected value [null]; codes [NotNull.saveUserForm.userExtId,NotNull.userExtId,NotNull.java.lang.Long,NotNull]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [saveUserForm.userExtId,userExtId]; arguments []; default message [userExtId]]; default message [不能为null]] ]
    /**
     ,MethodValidationPostProcessor.class
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }
     **/

    //@Primary
    //@Bean
    public ISensitiveStrategy sensitiveStrategy() {
        return new SensitiveStrategy()
                .addStrategy("custom",(String s)->{
                    return "自定义";
                });
    }

    @Bean
    public MateSimpleTransDiver mateSimpleTransDiver() {
        return new MateSimpleTransDiver();
    }


    public static class MateSimpleTransDiver implements SimpleTransService.SimpleTransDiver {
        @Override
        public List<? extends VO> findByIds(List<? extends Serializable> ids, Class<? extends VO> targetClass, String uniqueField) {
            return Collections.emptyList();
        }

        @Override
        public VO findById(Serializable id, Class<? extends VO> targetClass, String uniqueField) {
            UserExtInfo userExtInfo = new UserExtInfo();
            userExtInfo.setId(1L);
            userExtInfo.setDesc("我是描述");
            return userExtInfo;
        }
    }

}
