package com.github.bric3.spring;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target({TYPE, METHOD})
@Documented
@Conditional(OnPropertiesCollectionCondition.class)
public @interface ConditionalOnPropertiesCollection {
    String name();

    String[] subProperties();
}
