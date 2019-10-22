package com.github.bric3.spring;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static org.assertj.core.api.Assertions.assertThat;

public class OnCollectionPropertyConditionTest {

    private AnnotationConfigApplicationContext context;

    @After
    public void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    public void all_properties_are_defined() {
        load(AllSubPropertiesRequiredConfiguration.class,
             "property[0].sub-property1=value1",
             "property[0].sub-property2=value2");
        assertThat(context.containsBean("foo")).isTrue();
    }

    @Test
    public void not_all_properties_are_defined() {
        load(AllSubPropertiesRequiredConfiguration.class,
             "property[0].sub-property2=value2");
        assertThat(context.containsBean("foo")).isFalse();
    }

    @Test
    public void not_all_properties_in_collection_are_defined() {
        load(AllSubPropertiesRequiredConfiguration.class,
             "property[0].sub-property1=value01",
             "property[0].sub-property2=value02",
             "property[1].sub-property1=value11");
        assertThat(context.containsBean("foo")).isFalse();
    }

    @Test
    public void different_sub_properties_are_defined() {
        load(AllSubPropertiesRequiredConfiguration.class,
             "property.sub-property=value");
        assertThat(context.containsBean("foo")).isFalse();
    }

    @Test
    @Ignore("Spring RelaxedPropertyResolver does not handle relaxed properties well with indexes")
    public void relaxed_name() {
        load(RelaxedPropertyButNotSubPropertiesRequiredConfiguration.class,
             "theProperty[0].sub-property1=value1",
             "theProperty[0].sub-property2=value2");
        assertThat(context.containsBean("foo")).isTrue();
    }

    @Test
    public void meta_annotation_condition_matches_when_property_is_set() {
        load(MetaAnnotation.class,
             "my.feature[0].p1=value1",
             "my.feature[0].p2=value2");
        assertThat(context.containsBean("foo")).isTrue();
    }

    @Test
    public void meta_annotation_condition_does_not_match_when_property_is_not_set() {
        load(MetaAnnotation.class);
        assertThat(context.containsBean("foo")).isFalse();
    }

    @Test
    @Ignore("don't know why this doesn't work")
    public void meta_and_direct_annotation_condition_does_not_match_when_only_direct_property_is_set() {
        load(MetaAnnotationAndDirectAnnotation.class,
             "my.other.feature[one].p1=value1",
             "my.other.feature[one].p2=value2");
        assertThat(context.containsBean("foo")).isFalse();
    }

    @Test
    public void meta_and_direct_annotation_condition_does_not_match_when_only_meta_property_is_set() {
        load(MetaAnnotationAndDirectAnnotation.class,
             "my.feature[0].p1=value1",
             "my.feature[0].p2=value2");
        assertThat(context.containsBean("foo")).isFalse();
    }

    @Test
    public void meta_and_direct_annotation_condition_does_not_match_when_neither_property_is_set() {
        load(MetaAnnotationAndDirectAnnotation.class);
        assertThat(context.containsBean("foo")).isFalse();
    }

    @Test
    public void metaAndDirectAnnotationConditionMatchesWhenBothPropertiesAreSet() {
        load(MetaAnnotationAndDirectAnnotation.class,
             "my.feature[0].p1=value1",
             "my.feature[0].p2=value2",
             "my.other.feature[one].p1=value1",
             "my.other.feature[one].p2=value2");
        assertThat(context.containsBean("foo")).isTrue();
    }

    private void load(Class<?> config, String... environment) {
        context = new AnnotationConfigApplicationContext();
        EnvironmentTestUtils.addEnvironment(context, environment);
        context.register(config);
        context.refresh();
    }

    @Configuration
    @ConditionalOnPropertiesCollection(name = "property", subProperties = {"sub-property1", "sub-property2"})
    protected static class AllSubPropertiesRequiredConfiguration {
        @Bean
        public String foo() {
            return "foo";
        }
    }

    @Configuration
    @ConditionalOnPropertiesCollection(name = "the-property", subProperties = {"sub-property1", "sub-property2"})
    protected static class RelaxedPropertyButNotSubPropertiesRequiredConfiguration {
        @Bean
        public String foo() {
            return "foo";
        }
    }

    @ConditionalOnMyFeature
    protected static class MetaAnnotation {
        @Bean
        public String foo() {
            return "foo";
        }
    }

    @ConditionalOnMyFeature
    @ConditionalOnPropertiesCollection(name = "my.other.feature", subProperties = {"p1", "p2"})
    protected static class MetaAnnotationAndDirectAnnotation {
        @Bean
        public String foo() {
            return "foo";
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD})
    @ConditionalOnPropertiesCollection(name = "my.feature", subProperties = {"p1", "p2"})
    public @interface ConditionalOnMyFeature {
    }
}