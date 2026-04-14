/**
 * Project Name: license-manager
 * Author: Patrick Mutwiri <dev@patric.xyz>
 * Author URL: https://github.com/patricmutwiri
 * Date: 2026-04-14
 */

package com.mutwiri.licensemanager.configs;

import org.flywaydb.core.Flyway;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = true)
public class FlywaySchemaConfig {
    private static final String FLYWAY_INITIALIZER_BEAN = "licenseFlywayInitializer";

    @Bean
    Flyway licenseFlyway(DataSource dataSource,
            @Value("${spring.flyway.locations:classpath:db/migration}") String locations) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(splitLocations(locations))
                .load();
    }

    @Bean(name = FLYWAY_INITIALIZER_BEAN)
    FlywayInitializer licenseFlywayInitializer(Flyway licenseFlyway) {
        return new FlywayInitializer(licenseFlyway);
    }

    @Bean
    static BeanFactoryPostProcessor entityManagerDependsOnFlyway() {
        return beanFactory -> addDependency(beanFactory, "entityManagerFactory", FLYWAY_INITIALIZER_BEAN);
    }

    private static void addDependency(ConfigurableListableBeanFactory beanFactory,
            String beanName,
            String dependencyName) throws BeansException {
        try {
            BeanDefinition definition = beanFactory.getBeanDefinition(beanName);
            String[] existing = definition.getDependsOn();
            if (existing == null || existing.length == 0) {
                definition.setDependsOn(dependencyName);
                return;
            }
            String[] dependencies = java.util.Arrays.copyOf(existing, existing.length + 1);
            dependencies[existing.length] = dependencyName;
            definition.setDependsOn(dependencies);
        } catch (NoSuchBeanDefinitionException ignored) {
            // Non-JPA test slices may not register an EntityManagerFactory.
        }
    }

    private static String[] splitLocations(String locations) {
        return java.util.Arrays.stream(locations.split(","))
                .map(String::trim)
                .filter(location -> !location.isBlank())
                .toArray(String[]::new);
    }

    static final class FlywayInitializer {
        private final Flyway flyway;

        private FlywayInitializer(Flyway flyway) {
            this.flyway = flyway;
            this.flyway.migrate();
        }
    }
}
