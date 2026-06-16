package cn.smu.edu.common.aop;

import java.lang.annotation.*;

/**
 * 数据权限注解 — 防止跨院系越权读取数据（等保三级要求）
 * AOP 自动在 MyBatis-Plus 查询中追加 dept_id IN (...) 条件
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    String deptAlias() default "t";
    String userAlias() default "";
}
