package com.mymvc.v1.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyAutowired {

    String value() default "";
}
