package com.xiongx9527.fw.plus.boot.magicapi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitterReturnValueHandler;

import java.util.stream.Collectors;
//import org.springframework.cloud.config.server.EnableConfigServer;
//@EnableConfigServer
//@EnableApolloPropertiesProvider
@SpringBootApplication
public class MagicapiSampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(MagicapiSampleApplication.class, args);
    }
}
