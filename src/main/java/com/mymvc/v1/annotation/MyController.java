package com.mymvc.v1.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyController {

    String value() default "";
}
