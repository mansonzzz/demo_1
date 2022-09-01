package com.st.bean.spring;

import com.st.bean.TimestampConverter;
import ma.glasnost.orika.Converter;
import ma.glasnost.orika.Mapper;
import ma.glasnost.orika.MapperFactory;
import ma.glasnost.orika.converter.ConverterFactory;
import ma.glasnost.orika.converter.builtin.PassThroughConverter;
import ma.glasnost.orika.impl.ConfigurableMapper;
import ma.glasnost.orika.impl.DefaultMapperFactory;
import ma.glasnost.orika.metadata.ClassMapBuilder;
import ma.glasnost.orika.metadata.TypeFactory;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.List;
import java.util.Map;

/**
 * Orika mapper exposed as a Spring Bean. It contains the configuration for the mapper factory and factory builder. It will scan
 * the Spring application context searching for mappers and converters to register them into the factory. To use it we just need
 * to autowire it into our class.
 *
 * @author dlizarra
 */
public class OrikaBeanMapper extends ConfigurableMapper implements ApplicationContextAware {

	private MapperFactory factory;
	private ApplicationContext applicationContext;

	public OrikaBeanMapper() {
		super(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configure(MapperFactory factory) {
		this.factory = factory;
		addAllSpringBeans(applicationContext);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void configureFactoryBuilder(final DefaultMapperFactory.Builder factoryBuilder) {
		// Nothing to configure for now
	}

	/**
	 * Constructs and registers a {@link ClassMapBuilder} into the {@link MapperFactory} using a {@link Mapper}.
	 *
	 * @param mapper
	 */
	@SuppressWarnings("rawtypes")
	public void addMapper(Mapper<?, ?> mapper) {
		factory.classMap(mapper.getAType(), mapper.getBType())
				.byDefault()
				.customize((Mapper) mapper)
				.register();
	}

	/**
	 * Registers a {@link Converter} into the {@link ConverterFactory}.
	 *
	 * @param converter
	 */
	public void addConverter(Converter<?, ?> converter) {
		factory.getConverterFactory().registerConverter(converter);
	}

	/**
	 * Scans the appliaction context and registers all Mappers and Converters found in it.
	 *
	 * @param applicationContext
	 */
	@SuppressWarnings("rawtypes")
	private void addAllSpringBeans(final ApplicationContext applicationContext) {
		Map<String, MappingConfigurer> configurers = applicationContext.getBeansOfType(MappingConfigurer.class);
		for (MappingConfigurer configurer : configurers.values()) {
			configurer.configure(this.factory);
		}

		Map<String, Mapper> mappers = applicationContext.getBeansOfType(Mapper.class);
		for (Mapper mapper : mappers.values()) {
			addMapper(mapper);
		}
		Map<String, Converter> converters = applicationContext.getBeansOfType(Converter.class);
		for (Converter converter : converters.values()) {
			addConverter(converter);
		}
		addCustomConverter();
		this.factory.getConverterFactory().registerConverter(new PassThroughConverter(LocalDateTime.class, DateTime.class, DateTimeZone.class));
	}

	private void addCustomConverter() {
		factory.getConverterFactory().registerConverter("timestampConverter", new TimestampConverter());
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		init();
	}

	public <S, D> List<D> mapList(Iterable<S> sourceList, Class<S> sourceClass, Class<D> destinationClass) {
		return mapAsList(sourceList, TypeFactory.valueOf(sourceClass), TypeFactory.valueOf(destinationClass));
	}

}