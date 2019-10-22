package com.github.bric3.spring.autowire;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Component
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutowireExcluding {

    String qualifierToExcludeValue();

    Class<?> aClass();
}