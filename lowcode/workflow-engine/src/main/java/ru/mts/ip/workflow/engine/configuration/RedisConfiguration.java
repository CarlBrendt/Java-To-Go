package ru.mts.ip.workflow.engine.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.mts.ip.workflow.engine.esql.EsqlToLuaEvent;

@Configuration
public class RedisConfiguration {

//  @Bean
//  RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
//      RedisTemplate<String, Object> template = new RedisTemplate<>();
//      template.setConnectionFactory(connectionFactory);
//      template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
//      return template;
//  }
  
  @Bean
  RedisOperations<String, EsqlToLuaEvent> eventRedisOperations(RedisConnectionFactory redisConnectionFactory, ObjectMapper objectMapper) {
      Jackson2JsonRedisSerializer<EsqlToLuaEvent> jsonRedisSerializer = new Jackson2JsonRedisSerializer<>(objectMapper,
          EsqlToLuaEvent.class);
      RedisTemplate<String, EsqlToLuaEvent> eventRedisTemplate = new RedisTemplate<>();
      eventRedisTemplate.setConnectionFactory(redisConnectionFactory);
      eventRedisTemplate.setKeySerializer(RedisSerializer.string());
      eventRedisTemplate.setValueSerializer(jsonRedisSerializer);
      eventRedisTemplate.setHashKeySerializer(RedisSerializer.string());
      eventRedisTemplate.setHashValueSerializer(jsonRedisSerializer);
      return eventRedisTemplate;
  }

  @Bean
  RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory redisConnectionFactory) {
      RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
      redisMessageListenerContainer.setConnectionFactory(redisConnectionFactory);
      return redisMessageListenerContainer;
  }
  
  
}
