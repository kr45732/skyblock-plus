package com.skyblockplus.api.miscellaneous;

import static com.skyblockplus.utils.Utils.*;

import javax.sql.DataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

	@ConfigurationProperties(prefix = "application.properties")
	@Bean
	public DataSource getDataSource() {
		return DataSourceBuilder.create().url(DATABASE_URL).username(DATABASE_USERNAME).password(DATABASE_PASSWORD).build();
	}
}
