package cn.smu.edu.jwxt;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("cn.smu.edu.jwxt.repository")
@ComponentScan(basePackages = {"cn.smu.edu.jwxt", "cn.smu.edu.common"})
public class JwxtApplication {
    public static void main(String[] args) {
        SpringApplication.run(JwxtApplication.class, args);
    }
}
