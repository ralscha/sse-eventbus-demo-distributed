package ch.rasc.eventbus.demo.distributed;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import ch.rasc.sse.eventbus.config.EnableSseEventBus;

@SpringBootApplication
@EnableSseEventBus
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	RedisTemplate<String, byte[]> redisTemplate(RedisConnectionFactory factory) {
		RedisTemplate<String, byte[]> template = new RedisTemplate<>();
		template.setConnectionFactory(factory);
		template.setKeySerializer(new StringRedisSerializer());
		template.setValueSerializer(RedisSerializer.byteArray());
		return template;
	}

	@Bean
	RedisMessageListenerContainer redisListenerContainer(RedisConnectionFactory factory,
			RedisDistributedEventBus distributedEventBus) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(factory);
		container.addMessageListener(distributedEventBus, new ChannelTopic(RedisDistributedEventBus.CHANNEL));
		return container;
	}

}
