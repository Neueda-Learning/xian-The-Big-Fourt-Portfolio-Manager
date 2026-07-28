package org.example.xianthebigfourtportfoliomanager.config;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {
    // A dedicated RestTemplate for AI requests avoids interfering with other service clients.
    @Bean
    @Qualifier("aiRestTemplate")
    public RestTemplate aiRestTemplate(AiProperties aiProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // Timeouts come from configuration; fall back to sensible defaults when not set.
        requestFactory.setConnectTimeout(resolveTimeout(aiProperties.getConnectTimeout(), 5000));
        requestFactory.setReadTimeout(resolveTimeout(aiProperties.getReadTimeout(), 30000));
        return new RestTemplate(requestFactory);
    }
    private int resolveTimeout(Integer configured, int defaultValue) {
        if (configured == null || configured <= 0) {
            return defaultValue;
        }
        return configured;
    }
}