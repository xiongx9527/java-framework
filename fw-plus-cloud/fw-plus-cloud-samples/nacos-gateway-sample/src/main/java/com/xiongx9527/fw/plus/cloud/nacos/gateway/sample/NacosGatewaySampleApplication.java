package com.xiongx9527.fw.plus.cloud.nacos.gateway.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class NacosGatewaySampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(NacosGatewaySampleApplication.class, args);
    }
}