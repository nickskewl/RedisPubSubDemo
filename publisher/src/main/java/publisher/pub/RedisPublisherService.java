package publisher.pub;

import dto.Joke;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * @author nitesh
 */
@Service
@Slf4j
public class RedisPublisherService {

    private static final String JOKE_API_ENDPOINT = "https://joke.deno.dev/";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    @Qualifier("publisherRedisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ChannelTopic channelTopic;

    @Scheduled(fixedRate = 5000)
    public Long publish() {
        Joke joke = restTemplate.getForObject(JOKE_API_ENDPOINT, Joke.class);
        log.info("Sending message: \n{}", joke);
        assert joke != null;
        return redisTemplate.convertAndSend(channelTopic.getTopic(), joke);
    }
}
