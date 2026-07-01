package com.xx9527.fw.plus.boot.jdbc.sample;

//import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan(basePackages = {"com.xx9527.fw.plus.boot.jdbc.sample.mapper"})
@SpringBootApplication
public class JdbcSampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(JdbcSampleApplication.class, args);
        //org.springframework.validation.beanvalidation.LocalValidatorFactoryBean ff;
    }
}
