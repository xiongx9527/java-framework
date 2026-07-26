package eureka.server.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@EnableEurekaServer
//@EnableAdminServer
public class EurekaServerSampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerSampleApplication.class, args);
    }
}
