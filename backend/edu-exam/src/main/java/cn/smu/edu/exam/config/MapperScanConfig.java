package cn.smu.edu.exam.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * 仅负责本服务的 Mapper 扫描。
 * MyBatis-Plus 分页拦截器由 edu-common 的自动配置统一提供，此处不再重复定义；
 * 类名也不得与 edu-common 的 MybatisPlusConfig 同名，否则组件扫描时两者都注册
 * 为 bean 名 mybatisPlusConfig，触发 ConflictingBeanDefinitionException 导致启动失败。
 */
@Configuration
@MapperScan("cn.smu.edu.exam.repository")
public class MapperScanConfig {
}
