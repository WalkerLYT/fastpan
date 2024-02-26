package com.fastpan.annotation;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
// 该注解对应方法
@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@Mapping
public @interface GlobalInterceptor {

    /**
     * 是否校验登录
     * 事实上这里的“方法”叫“注解元素”，可以当成一个成员变量，类似于接口中的抽象方法声明
     */
    boolean checkLogin() default true;

    /**
     * 是否校验参数
     */
    boolean checkParams() default false;

    /**
     * 是否校验管理员
     */
    boolean checkAdmin() default false;
}
