package subscriber.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import subscriber.sub.RedisSubscriberService;

/**
 * @author nitesh
 */
@Configuration
public class SubscriberConfiguration {

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Autowired
    private RedisSubscriberService redisSubscriberService;

    @Value("${redis.pubsub.topic}")
    private String redisPubSubTopic;

    @Bean
    public ChannelTopic topic() {
        return new ChannelTopic(redisPubSubTopic);
    }

    /*
    contains a custom implementation of the MessageListener interface called RedisSubscriberService.
    This bean acts as a subscriber in the pub-sub messaging model
    */
    @Bean
    public MessageListenerAdapter messageListener() {
        return new MessageListenerAdapter(redisSubscriberService);
    }

    /*
    RedisMessageListenerContainer is a class provided by Spring Data Redis which provides asynchronous behavior for Redis message listeners.
    This is called internally and “handles the low level details of listening, converting and message dispatching.”
     */
    @Bean
    public RedisMessageListenerContainer redisContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListener(), topic());
        return container;
    }
}
