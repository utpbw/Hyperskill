package com.example.taskmanagement.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.SslStoreProvider;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Base64;

@Configuration
public class SslConfiguration {

    private static final String KEYSTORE_PASSWORD = "password";
    private static final String KEY_ALIAS = "accountant_service";
    private static final String KEYSTORE_BASE64 = """
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
""";

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> sslStoreProviderCustomizer()
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = loadKeyStore();
        KeyStore trustStore = createTrustStore(keyStore);
        configureDefaultTrustStore(trustStore);

        return factory -> {
            factory.setSsl(createSslConfiguration());
            factory.setSslStoreProvider(new InMemorySslStoreProvider(keyStore, trustStore));
        };
    }

    private KeyStore loadKeyStore() throws GeneralSecurityException, IOException {
        byte[] keyStoreBytes = Base64.getMimeDecoder().decode(KEYSTORE_BASE64);
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new ByteArrayInputStream(keyStoreBytes), KEYSTORE_PASSWORD.toCharArray());
        return keyStore;
    }

    private KeyStore createTrustStore(KeyStore keyStore) throws GeneralSecurityException, IOException {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        if (keyStore.getCertificate(KEY_ALIAS) == null) {
            throw new IllegalStateException("Certificate with alias '" + KEY_ALIAS + "' not found");
        }
        trustStore.setCertificateEntry(KEY_ALIAS, keyStore.getCertificate(KEY_ALIAS));
        return trustStore;
    }

    private void configureDefaultTrustStore(KeyStore trustStore) throws GeneralSecurityException, IOException {
        Path trustStorePath = Files.createTempFile("accountant_service_trust", ".p12");
        try (OutputStream outputStream = Files.newOutputStream(trustStorePath)) {
            trustStore.store(outputStream, KEYSTORE_PASSWORD.toCharArray());
        }

        trustStorePath.toFile().deleteOnExit();
        System.setProperty("javax.net.ssl.trustStore", trustStorePath.toAbsolutePath().toString());
        System.setProperty("javax.net.ssl.trustStorePassword", KEYSTORE_PASSWORD);
    }

    private Ssl createSslConfiguration() {
        Ssl ssl = new Ssl();
        ssl.setEnabled(true);
        ssl.setKeyAlias(KEY_ALIAS);
        ssl.setKeyPassword(KEYSTORE_PASSWORD);
        ssl.setKeyStorePassword(KEYSTORE_PASSWORD);
        ssl.setKeyStoreType("PKCS12");
        return ssl;
    }

    private record InMemorySslStoreProvider(KeyStore keyStore, KeyStore trustStore) implements SslStoreProvider {
        @Override
        public KeyStore getKeyStore() {
            return keyStore;
        }

        @Override
        public KeyStore getTrustStore() {
            return trustStore;
        }
    }
}
