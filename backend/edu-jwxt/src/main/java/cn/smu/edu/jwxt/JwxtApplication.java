package cn.smu.edu.jwxt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class JwxtApplication {
    public static void main(String[] args) {
        SpringApplication.run(JwxtApplication.class, args);
    }
}
