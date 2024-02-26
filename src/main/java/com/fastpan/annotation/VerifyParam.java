package com.fastpan.annotation;


import com.fastpan.entity.enums.VerifyRegexEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义校验参数的注解
 */
@Retention(RetentionPolicy.RUNTIME)
// 该注解对应方法的参数
@Target({ElementType.PARAMETER, ElementType.FIELD})
public @interface VerifyParam {
    // 校验正则
    VerifyRegexEnum regex() default VerifyRegexEnum.NO;

    // 最小长度
    int min() default -1;

    // 最大长度
    int max() default -1;

    boolean required() default false;
}
