package com.ynp.agent.config;



import com.ynp.agent.filter.JwtAuthFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class ServletFilterConfig {
    @Bean
    //springboot 提供的过滤器注册器，替代servlet方式
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterFilterRegistrationBean(JwtAuthFilter jwtAuthFilter){
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(jwtAuthFilter);
        //数值越低，优先级越高
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
        registration.addUrlPatterns("/*");
        registration.setName("jwtAuthFilter");
        return registration;
    }
}
