package com.xiongx9527.fw.plus.boot.mp.sample;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(basePackages = {"com.xiongx9527.fw.plus.boot.mp.sample.mapper"})
@SpringBootApplication
public class MpSampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(MpSampleApplication.class, args);
        //org.springframework.validation.beanvalidation.LocalValidatorFactoryBean ff;
    }
}
