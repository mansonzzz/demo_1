package com.st.bean.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrikaAutoConfiguration {

    @Bean
    OrikaBeanMapper orikaBeanMapper() {
        return new OrikaBeanMapper();
    }
    
}
