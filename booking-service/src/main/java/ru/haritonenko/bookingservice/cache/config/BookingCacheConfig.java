package ru.haritonenko.bookingservice.cache.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.haritonenko.bookingservice.domain.Booking;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Configuration
@EnableCaching
public class BookingCacheConfig implements CachingConfigurer {

    @Bean
    public CacheManager cacheManager(
            ObjectProvider<RedisConnectionFactory> connectionFactoryProvider,
            ObjectMapper objectMapper,
            BookingCacheProperties properties
    ) {
        RedisConnectionFactory connectionFactory = connectionFactoryProvider.getIfAvailable();
        if (connectionFactory == null) {
            log.warn("Redis connection factory is unavailable; using an in-memory booking cache");
            return new ConcurrentMapCacheManager("bookingByUser");
        }

        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.getTtl())
                .computePrefixWith(cacheName -> "booking-service:v1:" + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new JacksonJsonRedisSerializer<>(objectMapper, Booking.class)
                ));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(configuration)
                .withCacheConfiguration("bookingByUser", configuration)
                .transactionAware()
                .build();
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache read failed; falling back to PostgreSQL: cache={}, key={}",
                        cache.getName(), key, exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis cache write failed: cache={}, key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache eviction failed: cache={}, key={}", cache.getName(), key, exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis cache clear failed: cache={}", cache.getName(), exception);
            }
        };
    }
}
