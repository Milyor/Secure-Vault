package io.github.milyor.doc_storage_api.config;


import io.github.milyor.doc_storage_api.model.Users;
import io.github.milyor.doc_storage_api.repository.UserRep;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
        basePackages = "io.github.milyor.doc_storage_api.repository",
        includeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = UserRep.class
        ),
        entityManagerFactoryRef = "usersEntityManager",
        transactionManagerRef = "usersTransactionManager"
)
public class UsersDbConfig {

    @Bean
    @ConfigurationProperties(prefix = "spring.users-datasource")
    public DataSource usersDataSource() {
        return DataSourceBuilder.create().build();
    }


    @Bean
    public LocalContainerEntityManagerFactoryBean usersEntityManager(
            EntityManagerFactoryBuilder builder) {

        return builder
                .dataSource(usersDataSource())
                .packages(Users.class)
                .persistenceUnit("users")
                .build();
    }

    @Bean
    public PlatformTransactionManager usersTransactionManager(
            @Qualifier("usersEntityManager") EntityManagerFactory emf) {

        return new JpaTransactionManager(emf);
    }


}
