package com.khoavdse170395.notificationservice.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer Configuration.
 * 
 * Configures Kafka Consumer để nhận messages từ Kafka topics.
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:notification-service-group}")
    private String groupId;

    /**
     * Tạo ConsumerFactory với các config cần thiết.
     */
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        
        // Địa chỉ Kafka Broker
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        // Consumer Group ID - mỗi service có group riêng
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        
        // Auto commit offset (sau khi xử lý message)
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, 1000);
        
        // Auto offset reset: earliest = đọc từ đầu nếu chưa có offset
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // Deserialize value (Event object) từ JSON
        // Configure ObjectMapper để deserialize LocalDateTime đúng cách
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Support LocalDateTime, LocalDate, etc.
        
        // Configure JsonDeserializer với Object.class làm default type
        // Spring sẽ tự động detect type từ method parameter (@Payload PostCreatedEvent) trong @KafkaListener
        JsonDeserializer<Object> jsonDeserializer = new JsonDeserializer<>(Object.class, objectMapper);
        jsonDeserializer.setUseTypeHeaders(false); // Không dùng type headers từ message
        jsonDeserializer.setRemoveTypeHeaders(true); // Remove type headers nếu có
        jsonDeserializer.addTrustedPackages("*"); // Trust tất cả packages
        
        // Wrap với ErrorHandlingDeserializer để handle deserialization errors gracefully
        // ErrorHandlingDeserializer sẽ skip messages không deserialize được và tiếp tục
        ErrorHandlingDeserializer<Object> errorHandlingDeserializer = new ErrorHandlingDeserializer<>(jsonDeserializer);
        
        // Pass deserializers vào constructor
        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), errorHandlingDeserializer);
    }

    /**
     * Tạo ObjectMapper bean với JavaTimeModule để deserialize LocalDateTime.
     * Bean này sẽ được inject vào PostEventConsumer để convert LinkedHashMap sang Event objects.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }

    /**
     * Tạo KafkaListenerContainerFactory để xử lý messages.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        
        // Error handler để skip messages không deserialize được
        // Seek past failed records để tiếp tục với messages tiếp theo
        DefaultErrorHandler errorHandler = new DefaultErrorHandler();
        errorHandler.addNotRetryableExceptions(
            java.lang.IllegalStateException.class,
            org.apache.kafka.common.errors.RecordDeserializationException.class
        );
        factory.setCommonErrorHandler(errorHandler);
        
        return factory;
    }
}
