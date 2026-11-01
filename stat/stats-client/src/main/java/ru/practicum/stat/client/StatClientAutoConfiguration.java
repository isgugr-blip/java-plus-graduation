package ru.practicum.stat.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@AutoConfiguration
public class StatClientAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    StatClient statClient(RestClient.Builder builder,
                          DiscoveryClient discoveryClient,
                          @Value("${stats-client.service-id}") String statServiceId) {
        return new StatClient(builder.build(), discoveryClient, statServiceId);
    }
}
