package ru.haritonenko.catalogservice.photo.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.haritonenko.catalogservice.photo.category.domain.RoomCategoryPhoto;
import ru.haritonenko.catalogservice.photo.service.domain.ServiceItemPhoto;
import tools.jackson.databind.ObjectMapper;

@Configuration
@RequiredArgsConstructor
public class CachePhotoConfig {
    @Bean
    public RedisTemplate<String, RoomCategoryPhoto> redisPhotoTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, RoomCategoryPhoto> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        var keySerializer = new StringRedisSerializer();
        var valueSerializer = new JacksonJsonRedisSerializer<>(objectMapper, RoomCategoryPhoto.class);

        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();

        return template;
    }


    @Bean
    public RedisTemplate<String, ServiceItemPhoto> redisServicePhotoTemplate(
            RedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper
    ) {
        RedisTemplate<String, ServiceItemPhoto> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        var keySerializer = new StringRedisSerializer();
        var valueSerializer = new JacksonJsonRedisSerializer<>(objectMapper, ServiceItemPhoto.class);

        template.setKeySerializer(keySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashKeySerializer(keySerializer);
        template.setHashValueSerializer(valueSerializer);
        template.afterPropertiesSet();

        return template;
    }

    @Bean
    public StringRedisTemplate stringPhotoRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }
}
