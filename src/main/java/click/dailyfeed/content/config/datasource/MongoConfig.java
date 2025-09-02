package click.dailyfeed.content.config.datasource;

import click.dailyfeed.content.config.converter.BigDecimalToDecimal128Converter;
import click.dailyfeed.content.config.converter.Decimal128ToBigDecimalConverter;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.*;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Arrays;

@EnableTransactionManagement
@Configuration
@EnableMongoRepositories(
        basePackages = "click.dailyfeed.content.domain.**.repository.mongo",
        mongoTemplateRef = "mongoTemplate"
)
public class MongoConfig {
    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${app.services.content.mongodb.database}")
    private String database;

    @Bean
    public MongoClient mongoClient(){
        try {
            ConnectionString connectionString = new ConnectionString(mongoUri);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .build();
            return MongoClients.create(settings);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create MongoDB client", e);
        }
    }

    @Bean
    public MongoTransactionManager transactionManager(
            MongoDatabaseFactory dbFactory
    ){
        return new MongoTransactionManager(dbFactory);
    }

    @Bean
    public SimpleMongoClientDatabaseFactory dailyfeedMongoDatabaseFactory(
            MongoClient mongoClient
    ){
        return new SimpleMongoClientDatabaseFactory(mongoClient, database);
    }

    @Bean(name = "mongoTemplate")
    public MongoTemplate mongoTemplate(
            MongoDatabaseFactory dailyfeedMongoDatabaseFactory,
            MongoConverter mongoConverter
    ){
        return new MongoTemplate(dailyfeedMongoDatabaseFactory, mongoConverter);
    }

    @Bean
    public MongoCustomConversions mongoCustomConversions(){
        return new MongoCustomConversions(
                Arrays.asList(
                        new BigDecimalToDecimal128Converter(),
                        new Decimal128ToBigDecimalConverter()
                )
        );
    }

    @Bean
    public MongoMappingContext mongoMappingContext() {
        return new MongoMappingContext();
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter(
            MongoDatabaseFactory databaseFactory,
            MongoMappingContext mongoMappingContext
    ){
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(databaseFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return converter;
    }
}
