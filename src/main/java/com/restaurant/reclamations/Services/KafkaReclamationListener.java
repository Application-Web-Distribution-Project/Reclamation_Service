package com.restaurant.reclamations.Services;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class KafkaReclamationListener {

    @KafkaListener(topics = "reclamations", groupId = "reclamations-group")
    public void listenReclamationAddition(String message) {
        log.info("Received Kafka message: {}", message);
        // Process the message (e.g., log it, save it to the database, etc.)
    }
}