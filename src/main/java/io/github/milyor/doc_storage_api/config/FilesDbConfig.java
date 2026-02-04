package io.github.milyor.doc_storage_api.config;
import io.github.milyor.doc_storage_api.model.FileDocument;
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
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = UserRep.class
        ),
        entityManagerFactoryRef = "filesEntityManager",
        transactionManagerRef = "filesTransactionManager"
)
public class FilesDbConfig {

    @Primary
    @Bean
    @ConfigurationProperties(prefix = "spring.file-datasource")
    public DataSource filesDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Primary
    @Bean
    public LocalContainerEntityManagerFactoryBean filesEntityManager(
            EntityManagerFactoryBuilder builder) {

        return builder
                .dataSource(filesDataSource())
                .packages(FileDocument.class)
                .persistenceUnit("files")
                .build();
    }

    @Primary
    @Bean
    public PlatformTransactionManager filesTransactionManager(
            @Qualifier("filesEntityManager") EntityManagerFactory emf) {

        return new JpaTransactionManager(emf);
    }
}
