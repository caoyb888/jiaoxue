package cn.smu.edu.live;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LiveApplication {
    public static void main(String[] args) {
        SpringApplication.run(LiveApplication.class, args);
    }
}
