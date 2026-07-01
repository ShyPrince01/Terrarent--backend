package com.terrarent.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
@RequiredArgsConstructor
public class DatabaseConfig {

    private final Environment env;

    @Bean
    @Primary
    public DataSource dataSource() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null) {
            databaseUrl = System.getenv("SPRING_DATASOURCE_URL");
        }
        if (databaseUrl == null) {
            databaseUrl = env.getProperty("spring.datasource.url");
        }
        
        // If a standard postgres URI is provided (starts with postgres:// or postgresql://) instead of a JDBC URL
        if (databaseUrl != null && (databaseUrl.startsWith("postgres://") || databaseUrl.startsWith("postgresql://"))) {
            try {
                URI dbUri = new URI(databaseUrl);
                String username = null;
                String password = null;
                if (dbUri.getUserInfo() != null) {
                    String[] userInfo = dbUri.getUserInfo().split(":", 2);
                    username = userInfo[0];
                    if (userInfo.length > 1) {
                        password = userInfo[1];
                    }
                }
                
                // Construct standard Spring Boot JDBC URL
                String host = dbUri.getHost();
                int port = dbUri.getPort() == -1 ? 5432 : dbUri.getPort();
                String path = dbUri.getPath();
                
                String dbUrl = "jdbc:postgresql://" + host + ":" + port + path;

                System.out.println("🔌 Auto-configuring PostgreSQL connection from URI: " + dbUrl);
                
                DataSourceBuilder<?> builder = DataSourceBuilder.create()
                        .url(dbUrl)
                        .driverClassName("org.postgresql.Driver");
                
                if (username != null) {
                    builder.username(username);
                }
                if (password != null) {
                    builder.password(password);
                }
                
                return builder.build();
            } catch (Exception e) {
                System.err.println("❌ Failed to parse connection URI, falling back to raw configuration settings: " + e.getMessage());
            }
        }
        
        // Fallback: Use configuration values from application.yml
        System.out.println("🏠 Auto-configuring Database from application.yml settings!");
        return DataSourceBuilder.create()
                .url(env.getProperty("spring.datasource.url"))
                .username(env.getProperty("spring.datasource.username"))
                .password(env.getProperty("spring.datasource.password"))
                .driverClassName(env.getProperty("spring.datasource.driver-class-name", "org.postgresql.Driver"))
                .build();
    }
}
