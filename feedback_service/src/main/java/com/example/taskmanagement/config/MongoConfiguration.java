package com.example.taskmanagement.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.net.InetSocketAddress;

@Configuration
public class MongoConfiguration {

    @Bean(destroyMethod = "shutdown")
    @ConditionalOnProperty(name = "mongodb.embedded.enabled", havingValue = "true", matchIfMissing = true)
    public EmbeddedMongoServer embeddedMongoServer(
            @Value("${spring.data.mongodb.database:feedback}") String databaseName,
            @Value("${mongodb.embedded.host:127.0.0.1}") String host
    ) {
        MongoServer server = new MongoServer(new MemoryBackend());
        server.bind(host, 0);
        InetSocketAddress address = server.getLocalAddress();
        String connectionString = String.format("mongodb://%s:%d/%s", address.getHostString(), address.getPort(), databaseName);
        return new EmbeddedMongoServer(server, connectionString);
    }

    @Bean
    @ConditionalOnMissingBean
    public MongoClient mongoClient(
            Environment environment,
            ObjectProvider<EmbeddedMongoServer> embeddedMongoServerProvider
    ) {
        String configuredUri = environment.getProperty("spring.data.mongodb.uri");
        if ((configuredUri == null || configuredUri.isBlank())) {
            configuredUri = environment.getProperty("MONGODB_URI");
        }
        if (configuredUri != null && !configuredUri.isBlank()) {
            return MongoClients.create(configuredUri);
        }

        EmbeddedMongoServer embedded = embeddedMongoServerProvider.getIfAvailable();
        if (embedded != null) {
            return MongoClients.create(embedded.connectionString());
        }

        throw new IllegalStateException("MongoDB URI must be provided when embedded MongoDB is disabled");
    }

    public record EmbeddedMongoServer(MongoServer server, String connectionString) {
        public void shutdown() {
            server.shutdown();
        }
    }
}
