package com.example.iot_project.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    private final Dotenv dotenv = Dotenv.load();

    @Override
    protected String getDatabaseName() {
        return dotenv.get("MONGODB_DBNAME");
    }

    @Override
    @Bean
    public MongoClient mongoClient() {
        return MongoClients.create(dotenv.get("MONGODB_URI"));
    }
}
