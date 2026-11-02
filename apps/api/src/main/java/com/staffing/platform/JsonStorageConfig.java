package com.staffing.platform;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Share ISO-8601 JSON between HTTP, JSONB and the Python worker. */
@Configuration
public class JsonStorageConfig {
    @Bean HibernatePropertiesCustomizer jsonStorageMapper(ObjectMapper mapper) {
        return properties -> properties.put("hibernate.type.json_format_mapper", new JacksonJsonFormatMapper(mapper));
    }
}
