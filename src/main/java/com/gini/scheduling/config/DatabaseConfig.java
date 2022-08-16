package com.gini.scheduling.config;

import javax.sql.DataSource;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.ContextResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatWebServer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DatabaseConfig {
	@Value("${spring.datasource.jndiName}")
	private String jndiName;
	@Value("${spring.datasource.driverClassName}")
	private String DB_DRIVER;
	@Value("${spring.datasource.url}")
	private String DB_URL;
	@Value("${spring.datasource.username}")
	private String DB_USERNAME;
	@Value("${spring.datasource.password}")
	private String DB_PASSWORD;

	@Bean
	public ServletWebServerFactory servletContainer() {
		TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory() {
			@Override
			protected TomcatWebServer getTomcatWebServer(Tomcat tomcat) {
				tomcat.enableNaming();
				return super.getTomcatWebServer(tomcat);
			}

			@Override
			protected void postProcessContext(Context context) {
				ContextResource resource = new ContextResource();
				resource.setName(jndiName);
				resource.setType(DataSource.class.getName());
				resource.setProperty("driverClassName", DB_DRIVER);
				resource.setProperty("url", DB_URL);
				resource.setProperty("username", DB_USERNAME);
				resource.setProperty("password", DB_PASSWORD);
				context.getNamingResources().addResource(resource);
				super.postProcessContext(context);
			}
		};
		return tomcat;
	}
}