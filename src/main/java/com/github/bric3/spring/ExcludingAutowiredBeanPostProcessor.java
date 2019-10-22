package com.github.bric3.spring;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link org.springframework.beans.factory.config.BeanPostProcessor} implementation
 * that autowires annotated fields with {@link AutowireExcluding @AutowireExcluding} annotation.
 *
 * This post-processor only work with field injection.
 *
 * Adapted from the answer here https://stackoverflow.com/a/44685101/48136
 */
@Component
public class ExcludingAutowiredBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware {

    private ConfigurableListableBeanFactory beanFactory;

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        Arrays.stream(bean.getClass().getDeclaredFields()).forEach(field -> {
            AutowireExcluding myAutowiredExcludeAnnotation = field.getAnnotation(AutowireExcluding.class);
            if (myAutowiredExcludeAnnotation == null) {
                return;
            }

            Collection<Object> beanForInjection =
                    beanFactory.getBeansOfType(collectionType(myAutowiredExcludeAnnotation))
                               .values()
                               .stream()
                               .filter(beanCandidate -> {
                                   Qualifier qualifierForBeanCandidate = beanCandidate.getClass().getDeclaredAnnotation(Qualifier.class);
                                   return qualifierForBeanCandidate == null
                                          || !Objects.equals(qualifierForBeanCandidate.value(),
                                                             myAutowiredExcludeAnnotation.qualifierToExcludeValue());
                               })
                               .collect(Collectors.toList());

            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, bean, beanForInjection);
        });

        return bean;
    }

    private Class<?> collectionType(AutowireExcluding myAutowiredExcludeAnnotation) {
        return myAutowiredExcludeAnnotation.aClass();
    }


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) {
        if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {
            throw new IllegalArgumentException(
                    "AutowiredAnnotationBeanPostProcessor requires a ConfigurableListableBeanFactory: " + beanFactory);
        }
        this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
    }
}
