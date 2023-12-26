**What are Streaming Systems?**  
Streaming systems are message based systems, that handle a flow of events in an organized way. They are used to passing on these events between different parts of a system without making them too dependent on each other.

**Why do we need this?**  
Ever had to handle a lot of things when you make an online order? like sending notifications, emails, assigning deliveries, and more.
If we do all this in a single API request scope, then this particular API would take much time to complete all API calls to every external service.

How does Streaming Systems Help?  
Streaming system breaks the system into at-least 3 parts.
1. Source: Where events start (Producer, Publisher, etc)
2. Buffer or Queue: System where the events are stored or routed (Topics, Queues, PubSubs).
3. Sink: Where these events end up (Consumer, Subscriber, etc)

Three most used streaming systems:
1. Queue - Example: AWS SQS, RabbitMQ
   - Queue is like point-to-point communication. When data is sent to a queue, it is typically consumed by only one consumer.
   - Follow FIFO. 
   - A buffer like system, It stores the messages in the memory till it is not read and deleted (by reader) or deleted based on retention policy (stale data cleanup). 
   - Use case: When you want to build an event driven system, not wait for other services to complete processing before the current service can complete the process (async systems), and also need to store the message securely.
2. Topic - Example: Apache Kafka
   - Topics are like broadcast channels. When data is published to a topic, it goes to all subscribers interested in that topic.
   - Subscribers can consume messages independently and asynchronously.
   - A Buffer like system, where consumer does not delete the message after reading it.
   - Use case: Stock market app publish updates on different topics like "tech stock", "Energy Stock", etc and multiple subscriber can subscribe to different topics as per their interest.
3. PubSub - Example: Redis pubsub, AWS SNS
   - PubSub stands for "publish-subscribe"
   - Queues and Topics were buffer like Queue Systems, PubSub is a Real Time Broadcast system with no memory/buffer to store any data.
   - There are publishers that send messages without specifically identifying recipients, and there are subscribers that express interest in receiving messages of particular types or from specific publishers.
   - If the Subscriber is not connected or connects after some moments, it will not receive the messages already broadcast.
   - Use case: Just imagine it as a Loud Speaker or a Live Broadcast, where one message received by one publisher is sent to each subscriber/listener actively connected at that moment.
   
Each type has its own strengths, like how queues are good for making sure everything gets processed, topics are great for broadcasting updates, and PubSub is like a live broadcast where you need real-time communication.

**PubSub implementation**  
Let's build a pubsub based messaging system in Java using Redis pubsub.
In this, a publisher will publish a joke on a channel in every 5 sec and a subscriber subscribed to this channel with listen to these joke and print them.

We will use the "https://joke.deno.dev" URL to get random jokes and publish it.

Let's start, Use https://start.spring.io/ to build Spring boot project with below configuration:
- Maven Project
- Jar packaging
- Java 17 
- Dependencies:
   1. spring-boot-starter-data-redis
   2. spring-boot-starter-web
   3. lombok
   4. devtools

Import this project in intellij and will create below 3 modules:  
**1. Create a module common-dto**  
   - A simple Joke class is created.
   ```
   @Data
   public class Joke {

    private static final String JOKE_FORMAT = "Q: %s \nA: %s";

    private String setup;
    private String punchline;

    @Override
    public String toString() {
        return String.format(JOKE_FORMAT, this.setup, this.punchline);
    }
   }
   ```
**2. Create a module publisher**  
   1. Publisher Application:
   ```
   @SpringBootApplication
   @EnableScheduling
   public class PublisherApplication {
   public static void main(String[] args) {
   SpringApplication.run(PublisherApplication.class, args);
   }
   }
   ```
   2. In application.properties:
   ```
   server.port=8080
   redis.pubsub.topic=joke-events
   ```
   3. Define Redis configuration:
      - Redis Template: to interact with Redis.
      - Channel Topic: on which events are sent.
      - Rest Template: HTTP client to make rest API calls.
   ```
   @Configuration
   public class RedisConfiguration {
   
       @Value("${redis.pubsub.topic}")
       private String redisPubSubTopic;
   
       @Bean
       public ChannelTopic topic() {
           return new ChannelTopic(redisPubSubTopic);
       }
   
       @Bean(name = "publisherRedisTemplate")
       public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
           RedisTemplate<String, Object> template = new RedisTemplate<>();
           template.setConnectionFactory(connectionFactory);
           template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
           return template;
       }
   
       @Bean
       public RestTemplate restTemplate() {
           return new RestTemplate();
       }
   }
   ```
   4. Publisher Service:
      - publish method is scheduled to get a joke every 5 second using RestTemplate.
      - Publisher uses the convertAndSend() method to format and publish 'Joke' to the configured channel topic.
   ```
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
   ```
**3. Create a module Subscriber:**  
   1. Subscriber Application:
   ```
   @SpringBootApplication
   @EnableScheduling
   public class SubscriberApplication {
   public static void main(String[] args) {
   SpringApplication.run(SubscriberApplication.class, args);
   }
   }
   ```
   2. In application.properties:
         ```
         server.port=8081
         redis.pubsub.topic=joke-events
         ```
   3. Subscriber service:
      - Events would arrive at the Message Listener and from there we can use the event as we need. We need to implement onMessage method of MessageListener interface.
   ```
   @Service
   @Slf4j
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
   ```
   4. Subscriber configuration:
   - To use MessageListener we need to add this Message Listener to RedisMessageListenerContainer.
      1. MessageListenerAdapter: contains a custom implementation of the MessageListener interface called RedisSubscriberService.
         This bean acts as a subscriber in the pub-sub messaging model.
      2. RedisMessageListenerContainer: RedisMessageListenerContainer is a class provided by Spring Data Redis which provides asynchronous behavior for Redis message listeners.
         This is called internally and handles the low level details of listening, converting and message dispatching.

   ```
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

    @Bean
    public MessageListenerAdapter messageListener() {
        return new MessageListenerAdapter(redisSubscriberService);
    }

    @Bean
    public RedisMessageListenerContainer redisContainer() {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListener(), topic());
        return container;
    }
   }
   ```
   
Now, run the publisher and subscriber application to see the application in action.


**Publisher App console:**
```
2023-12-26T12:10:42.589+05:30  INFO 13523 --- [  restartedMain] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8080 (http) with context path ''
2023-12-26T12:10:42.599+05:30  INFO 13523 --- [  restartedMain] publisher.PublisherApplication           : Started PublisherApplication in 2.233 seconds (process running for 2.687)
2023-12-26T12:10:43.006+05:30  INFO 13523 --- [   scheduling-1] publisher.pub.RedisPublisherService   : Sending message: 
Q: Why did the barber win the race? 
A: He took a short cut.
2023-12-26T12:10:47.624+05:30  INFO 13523 --- [   scheduling-1] publisher.pub.RedisPublisherService   : Sending message: 
Q: Why are skeletons so calm? 
A: Because nothing gets under their skin.
2023-12-26T12:10:52.617+05:30  INFO 13523 --- [   scheduling-1] publisher.pub.RedisPublisherService   : Sending message: 
Q: Want to hear a chimney joke? 
A: Got stacks of em! First one's on the house

```

**Subscriber App console:**
```
2023-12-26T12:10:32.788+05:30  INFO 13520 --- [  restartedMain] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat started on port 8081 (http) with context path ''
2023-12-26T12:10:33.148+05:30  INFO 13520 --- [  restartedMain] subscriber.SubscriberApplication         : Started SubscriberApplication in 2.538 seconds (process running for 2.96)
Q: Why did the barber win the race? 
A: He took a short cut.
Q: Why are skeletons so calm? 
A: Because nothing gets under their skin.
Q: Want to hear a chimney joke? 
A: Got stacks of em! First one's on the house
```

References:  
https://www.linkedin.com/pulse/streaming-system-series-part-1-topics-queues-pubsubs-shrey-batra  
https://www.baeldung.com/spring-data-redis-pub-sub  
https://www.vinsguru.com/redis-pubsub-spring-boot  
