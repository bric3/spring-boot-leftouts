package com.github.bric3.spring.webmvc;

import com.github.bric3.spring.webmvc.SpringWebMvcPlusJerseyHackConfiguration.WebMvcEnforcedPrefixesProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletPath;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.util.UrlPathHelper;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME;

/**
 * Configuration that configures Spring Web MVC to work alongside Jersey from the same root path.
 *
 * <p>The issue is that when we want Spring WebMVC and Jersey to be on the same roo path,
 * e.g. `/`, Spring Boot by default register Jersey url-mapping with `/*` and as such
 * captures all request unless the JEE container is configured otherwise.
 * Either via a servlet filter, or via servlet url-mappings.</p>
 *
 * <p>This configuration is about the using the servlet url-mapping, by
 * registering the Spring Web MVC `DispatcherServlet` with a regular
 * `ServletRegistrationBean` that is configured with the wanted url-mappings.</p>
 *
 * <p>However Spring Web MVC by default uses some knowledge of the servlet
 * configuration to map request to actual handler. It is done via the
 * `UrlPathHelper` class that is used in many places (a new instance) with
 * its default configuration. This configuration sets a `UrlPathHelper`
 * configured to <i>always use full path</i> on various types that are part
 * of Spring Web MVC.</p>
 *
 *
 * https://github.com/spring-projects/spring-boot/issues/17523
 * https://github.com/bric3/jersey-webmvc
*/
@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureBefore(DispatcherServletAutoConfiguration.class)
@ConditionalOnClass(ServletRegistration.class)
@EnableConfigurationProperties({WebMvcProperties.class, WebMvcEnforcedPrefixesProperties.class})
@Profile("web-mvc-config-hack-mitigation")
public class SpringWebMvcPlusJerseyHackConfiguration {
    private final WebMvcProperties webMvcProperties;
    private final MultipartConfigElement multipartConfig;

    static {
        // has to be done before WebMvcEndpointHandlerMapping.afterPropertiesSet
        ReflectionUtils.doWithFields(
                WebMvcEndpointHandlerMapping.class,
                field -> {
                    ReflectionUtils.makeAccessible(field);
                    final RequestMappingInfo.BuilderConfiguration builderConfiguration =
                            (RequestMappingInfo.BuilderConfiguration) ReflectionUtils.getField(field, null);
                    Assert.notNull(builderConfiguration, "This code expects this '" + field + "' to be not null");
                    builderConfiguration.setUrlPathHelper(urlPathHelper());
                },
                field -> Objects.equals("builderConfig", field.getName()));
    }

    public SpringWebMvcPlusJerseyHackConfiguration(WebMvcProperties webMvcProperties,
                                                   ObjectProvider<MultipartConfigElement> multipartConfigProvider) {
        this.webMvcProperties = webMvcProperties;
        this.multipartConfig = multipartConfigProvider.getIfAvailable();
    }

    @Bean
    WebMvcConfigurer useFullPath() {
        return new WebMvcConfigurer() {
            @Override
            public void configurePathMatch(PathMatchConfigurer configurer) {
                final UrlPathHelper urlPathHelper = urlPathHelper();
                configurer.setUrlPathHelper(urlPathHelper);
            }
        };
    }

    @Bean
    Object handlerMappingCustomizer(List<AbstractHandlerMapping> handlerMappings) {
        handlerMappings.forEach(handlerMapping -> handlerMapping.setUrlPathHelper(urlPathHelper()));
        return true;
    }

    private static UrlPathHelper urlPathHelper() {
        final UrlPathHelper urlPathHelper = new UrlPathHelper();
        urlPathHelper.setAlwaysUseFullPath(true);
        return urlPathHelper;
    }

    @Bean(name = DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_REGISTRATION_BEAN_NAME)
    ServletRegistrationBean<DispatcherServlet> dispatcherServletRegistration(DispatcherServlet dispatcherServlet,
                                                                             WebMvcEnforcedPrefixesProperties webMvcEnforcedPrefixesProperties) {
        ServletRegistrationBean<DispatcherServlet> registration =
                new ServletRegistrationBean<>(dispatcherServlet,
                                              webMvcEnforcedPrefixesProperties.getUrlMappings()
                                                                              .stream()
                                                                              .toArray(String[]::new));
        registration.setName(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
        registration.setLoadOnStartup(this.webMvcProperties.getServlet().getLoadOnStartup());
        if (this.multipartConfig != null) {
            registration.setMultipartConfig(this.multipartConfig);
        }
        return registration;
    }

    @Bean
    DispatcherServletPath dispatcherServletPath() {
        final String path = this.webMvcProperties.getServlet().getPath();
        return () -> path;
    }

    @SuppressWarnings("ConfigurationProperties")
    @ConfigurationProperties("spring.mvc")
    public static class WebMvcEnforcedPrefixesProperties {
        private Set<String> urlMappings;

        public Set<String> getUrlMappings() {
            return urlMappings;
        }

        public void setUrlMappings(Set<String> urlMappings) {
            this.urlMappings = urlMappings;
        }
    }
}
