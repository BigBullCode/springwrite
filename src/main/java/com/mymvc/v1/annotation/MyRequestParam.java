package com.mymvc.v1.annotation;

import java.lang.annotation.*;

/**
 * 自定义注解
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestParam {

    String value() default "";
}
