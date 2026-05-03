package ru.haritonenko.catalogservice.category.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.haritonenko.catalogservice.category.domain.RoomCategory;

@Profile("dev")
@Configuration
@RequiredArgsConstructor
public class CacheCategoryConfig {

    @Bean
    public RedisTemplate<String, RoomCategory> redisCategoryTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, RoomCategory> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        var keySerializer = new StringRedisSerializer();
        var valueSerializer = new Jackson2JsonRedisSerializer<>(objectMapper, RoomCategory.class);

        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public StringRedisTemplate stringCategoryRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
