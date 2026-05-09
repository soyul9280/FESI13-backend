package com.fesi.deadlinemate.global.monitoring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

@Configuration
@Profile("local")
public class QueryLoggingBeanPostProcessor {

    // static: BeanPostProcessor must be instantiated before @Configuration beans to avoid lifecycle issues
    @Bean
    public static BeanPostProcessor dataSourceWrappingPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName)
                    throws BeansException {
                if (bean instanceof DataSource ds
                        && !(bean instanceof QueryCountingDataSourceWrapper)) {
                    return new QueryCountingDataSourceWrapper(ds);
                }
                return bean;
            }
        };
    }
}
