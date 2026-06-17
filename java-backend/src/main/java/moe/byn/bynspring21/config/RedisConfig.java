package moe.byn.bynspring21.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Value("${myapp.identifier}")
    private String keyPrefix;

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        RedisSerializer<String> prefixKeySerializer = new StringRedisSerializer() {
            @Override
            public byte[] serialize(String key) {
                if (key == null) {
                    return null;
                }
                String prefixedKey = keyPrefix + ":" + key;
                return super.serialize(prefixedKey);
            }

            @Override
            public String deserialize(byte[] bytes) {
                String key = super.deserialize(bytes);
                if (key != null && key.startsWith(keyPrefix + ":")) {
                    return key.substring(keyPrefix.length() + 1);
                }
                return key;
            }
        };

        GenericJackson2JsonRedisSerializer valueSerializer = createRedisSerializer();

        template.setKeySerializer(prefixKeySerializer);
        template.setHashKeySerializer(prefixKeySerializer);
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        template.afterPropertiesSet();
        return template;
    }

    private GenericJackson2JsonRedisSerializer createRedisSerializer() {
        ObjectMapper om = new ObjectMapper();
        om.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(om);
    }
}
