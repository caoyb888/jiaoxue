package cn.smu.edu.ai;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * 仅扫描 @Mapper 标注的 MyBatis 接口：repository 包内同时存在 Spring Data Mongo
 * 仓库接口（AiReviewResultRepository 等）。若不加 annotationClass 过滤，MapperScan
 * 会把 Mongo 仓库当作 MyBatis Mapper 注册，与 MongoRepositoryFactoryBean 冲突
 * （ConflictingBeanDefinitionException），导致应用无法启动。
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan(basePackages = "cn.smu.edu.ai.repository", annotationClass = Mapper.class)
@ComponentScan(basePackages = {"cn.smu.edu.ai", "cn.smu.edu.common"})
public class AiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AiApplication.class, args);
    }
}
