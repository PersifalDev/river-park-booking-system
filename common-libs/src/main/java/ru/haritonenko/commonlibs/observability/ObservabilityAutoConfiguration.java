package ru.haritonenko.commonlibs.observability;

import jakarta.servlet.Filter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.Ordered;
import ru.haritonenko.commonlibs.security.internal.InternalServiceAuthFilter;

@AutoConfiguration
@ConditionalOnClass(Filter.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class ObservabilityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RequestIdFilter.class)
    RequestIdFilter requestIdFilter() {
        return new RequestIdFilter();
    }

    @Bean
    FilterRegistrationBean<RequestIdFilter> requestIdFilterRegistration(RequestIdFilter requestIdFilter) {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(requestIdFilter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    @ConditionalOnMissingBean(InternalServiceAuthFilter.class)
    InternalServiceAuthFilter internalServiceAuthFilter(Environment environment) {
        return new InternalServiceAuthFilter(environment);
    }

    @Bean
    FilterRegistrationBean<InternalServiceAuthFilter> internalServiceAuthFilterRegistration(
            InternalServiceAuthFilter internalServiceAuthFilter
    ) {
        FilterRegistrationBean<InternalServiceAuthFilter> registration = new FilterRegistrationBean<>(internalServiceAuthFilter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.addUrlPatterns("/api/v1/internal/*");
        return registration;
    }
}
