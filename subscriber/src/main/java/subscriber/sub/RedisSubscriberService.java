package subscriber.sub;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.Joke;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * @author nitesh
 */
@Service
@Slf4j
/*
Events would arrive at the Message Listener and from there we can use the event as we need.
We need to implement onMessage method of MessageListener interface.
 */
public class RedisSubscriberService implements MessageListener {

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
//            log.info("New message received: {}", message);
            Joke joke = objectMapper.readValue(message.getBody(), Joke.class);
            System.out.println(joke);
        } catch (IOException e) {
            log.error("error while parsing message");
        }
    }
}
