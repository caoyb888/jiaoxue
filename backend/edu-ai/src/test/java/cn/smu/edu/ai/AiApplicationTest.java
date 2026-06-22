package cn.smu.edu.ai;

import org.apache.ibatis.annotations.Mapper;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 防回归：repository 包内同时含 MyBatis Mapper 与 Spring Data Mongo 仓库，
 * MapperScan 必须以 annotationClass=@Mapper 过滤，否则启动时 Mongo 仓库会被
 * 误注册为 Mapper，触发 ConflictingBeanDefinitionException。
 * （完整上下文启动需 MySQL/Mongo/Kafka/Nacos，不适合 CI，故以注解断言守护。）
 */
class AiApplicationTest {

    @Test
    void mapperScan_shouldFilterByMapperAnnotation() {
        MapperScan ms = AiApplication.class.getAnnotation(MapperScan.class);
        assertThat(ms).isNotNull();
        assertThat(ms.annotationClass())
                .as("MapperScan 必须只扫描 @Mapper 接口，避免与 Mongo 仓库冲突")
                .isEqualTo(Mapper.class);
    }
}
