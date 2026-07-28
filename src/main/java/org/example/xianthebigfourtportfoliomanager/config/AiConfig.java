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

    // 单独给 AI 请求提供 RestTemplate，避免影响其他业务服务。
    @Bean
    @Qualifier("aiRestTemplate")
    public RestTemplate aiRestTemplate(AiProperties aiProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        // 超时时间来自配置，未配置时使用默认值。
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

