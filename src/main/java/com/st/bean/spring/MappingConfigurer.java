package com.st.bean.spring;

import ma.glasnost.orika.MapperFactory;

public interface MappingConfigurer {
    void configure(MapperFactory mapperFactory);
}
