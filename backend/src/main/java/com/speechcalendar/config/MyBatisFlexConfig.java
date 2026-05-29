package com.speechcalendar.config;

import com.speechcalendar.mapper.EventMapper;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.mapper.MapperFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisFlexConfig {

    @Bean
    public MapperFactoryBean<EventMapper> eventMapper(SqlSessionFactory sqlSessionFactory) {
        MapperFactoryBean<EventMapper> factoryBean = new MapperFactoryBean<>(EventMapper.class);
        factoryBean.setSqlSessionFactory(sqlSessionFactory);
        return factoryBean;
    }
}
