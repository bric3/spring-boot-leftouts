package com.github.bric3.spring;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = ExclusionConditionTest.ConfigurationBean.class)
public class ExclusionConditionTest {

    @Autowired
    private ApplicationContext context;
    @Autowired
    private BeanExcluded beanExcluded;

    @AutowireExcluding(qualifierToExcludeValue = "excludedBean", aClass = ParentBean.class)
    private List<ParentBean> beansWithoutExclude;

    @Test
    public void should_not_inject_excluded_bean() {
        assertThat(context.getBeansOfType(ParentBean.class).values())
                .hasOnlyElementsOfTypes(Bean1Included.class,
                                        Bean2Included.class,
                                        Bean3Included.class,
                                        BeanExcluded.class);

        assertThat(beansWithoutExclude)
                .hasOnlyElementsOfTypes(Bean1Included.class,
                                        Bean2Included.class,
                                        Bean3Included.class)
                .doesNotHaveAnyElementsOfTypes(BeanExcluded.class);

        assertThat(beanExcluded).isNotNull();
    }

    @Configuration
    static class ConfigurationBean {

        @Bean
        public Bean1Included bean1(){
            return new Bean1Included();
        }

        @Bean
        public Bean2Included bean2(){
            return new Bean2Included();
        }

        @Bean
        public Bean3Included bean3(){
            return new Bean3Included();
        }

        @Bean
        public BeanExcluded excludedBean(){
            return new BeanExcluded();
        }

        @Bean
        public ExcludingAutowiredBeanPostProcessor excludeAutowiredBeanPostProcessor(){
            return new ExcludingAutowiredBeanPostProcessor();
        }
    }
}