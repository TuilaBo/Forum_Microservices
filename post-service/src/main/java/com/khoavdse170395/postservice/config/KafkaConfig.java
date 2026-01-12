package com.khoavdse170395.postservice.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Configuration cho Producer.
 * 
 * Kafka Producer là component gửi messages lên Kafka Broker.
 * 
 * Concepts:
 * 1. ProducerFactory: Tạo KafkaProducer instances
 * 2. KafkaTemplate: Wrapper để dễ dàng gửi messages (giống JdbcTemplate cho database)
 * 3. Serialization: Convert Java objects thành bytes để gửi qua network
 *    - Key: String (postId)
 *    - Value: JSON (Event object)
 * 
 * Configuration:
 * - bootstrap.servers: Địa chỉ Kafka Broker (localhost:9092)
 * - key.serializer: Serialize key (String)
 * - value.serializer: Serialize value (JSON)
 * - acks: Đảm bảo message được ghi vào broker (all = tất cả replicas)
 */
@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Tạo ProducerFactory với các config cần thiết.
     * ProducerFactory tạo ra KafkaProducer instances để gửi messages.
     */
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Địa chỉ Kafka Broker
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        
        // Serialize key (postId) thành String
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        
        // Đảm bảo message được ghi vào tất cả replicas (high durability)
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        
        // Retry khi gửi message thất bại
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        
        // Idempotent producer: đảm bảo không gửi duplicate messages
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Serialize value (Event object) thành JSON
        // Configure JsonSerializer để không gửi type information trong headers
        // Để các services khác có thể deserialize vào class của riêng chúng
        JsonSerializer<Object> jsonSerializer = new JsonSerializer<>();
        jsonSerializer.setAddTypeInfo(false); // Không thêm type information vào headers
        
        // Tạo ProducerFactory với custom serializer
        DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(configProps);
        factory.setValueSerializer(jsonSerializer);
        return factory;
    }

    /**
     * Tạo KafkaTemplate để dễ dàng gửi messages.
     * KafkaTemplate là high-level API, giống như JdbcTemplate cho database.
     */
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
