package click.dailyfeed.content;

import click.dailyfeed.content.domain.post.repository.mongo.PostMongoRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableCaching
@EnableJpaAuditing
@EnableMongoAuditing
@SpringBootApplication
@EnableJpaRepositories(
        basePackages = "click.dailyfeed.content.domain.**.repository.jpa",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager",
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = PostMongoRepository.class
        )
)
@EnableTransactionManagement
@EnableMongoRepositories(
        basePackages = "click.dailyfeed.content.domain.**.repository.mongo",
        mongoTemplateRef = "mongoTemplate"
)
@ComponentScan(basePackages = {
        "click.dailyfeed.feign",
        "click.dailyfeed.content",
		"click.dailyfeed.pagination",
		"click.dailyfeed.redis",
})
public class ContentApplication {

	public static void main(String[] args) {
		SpringApplication.run(ContentApplication.class, args);
	}

}
