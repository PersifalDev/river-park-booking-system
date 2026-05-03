package ru.haritonenko.catalogservice.services.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.haritonenko.catalogservice.photo.service.domain.ServiceItemPhoto;
import ru.haritonenko.catalogservice.services.domain.ServiceItem;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Profile("dev")
@EnableCaching
@Configuration
public class ServicesCacheConfig {


    @Value("${app.cache.default-ttl:30s}")
    private Duration defaultTtl;
    @Value("${app.cache.services-ttl:30s}")
    private Duration servicesTtl;

    @Bean
    public ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        return mapper;
    }

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory connectionFactory,
            ObjectMapper redisObjectMapper
    ) {
        var keySerializer = new StringRedisSerializer();

        RedisCacheConfiguration baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(defaultTtl)
                .computePrefixWith(cacheName -> "catalog-service:v1:" + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(keySerializer));

        Map<String, RedisCacheConfiguration> perCache = new HashMap<>();

        perCache.put("services",
                cacheConfig(baseConfig, servicesTtl, serializer(redisObjectMapper, ServiceItem.class)));
        perCache.put("servicesPages",
                cacheConfig(baseConfig, servicesTtl, listSerializer(redisObjectMapper, ServiceItem.class)));
        perCache.put("servicePhotos",
                cacheConfig(baseConfig, servicesTtl, serializer(redisObjectMapper, ServiceItemPhoto.class)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(perCache)
                .transactionAware()
                .build();
    }

    private <T> Jackson2JsonRedisSerializer<T> serializer(
            ObjectMapper mapper,
            Class<T> type
    ) {
        return new Jackson2JsonRedisSerializer<>(mapper, type);
    }

    private <T> Jackson2JsonRedisSerializer<T> listSerializer(
            ObjectMapper mapper,
            Class<T> elementType
    ) {
        var javaType = mapper.getTypeFactory()
                .constructCollectionType(List.class, elementType);
        return new Jackson2JsonRedisSerializer<>(mapper, javaType);
    }

    private RedisCacheConfiguration cacheConfig(
            RedisCacheConfiguration base,
            Duration ttl,
            Jackson2JsonRedisSerializer<?> valueSerializer
    ) {
        return base.entryTtl(ttl)
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(valueSerializer)
                );
    }
}
