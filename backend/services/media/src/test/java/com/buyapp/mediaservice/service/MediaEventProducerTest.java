package com.buyapp.mediaservice.service;

import com.buyapp.common.event.MediaEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static com.buyapp.common.event.MediaEvent.EventType.*;

@ExtendWith(MockitoExtension.class)
class MediaEventProducerTest {

    @Mock
    private KafkaTemplate<String, MediaEvent> kafkaTemplate;

    private MediaEventProducer producer;

    @BeforeEach
    void setUp() {
        producer = new MediaEventProducer(kafkaTemplate);
    }

    @Test
    void sendsEventUsingKafkaTemplate() {
        // Arrange: mock kafkaTemplate to return a completed future
        CompletableFuture<SendResult<String, MediaEvent>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(nullable(String.class), anyString(), any(MediaEvent.class)))
                .thenReturn(future);

        // Create event
        MediaEvent event = new MediaEvent();
        event.setMediaId("m-1");
        event.setEventType(IMAGE_UPLOADED);

        // Act
        producer.sendMediaEvent(event);

        // Assert: verify send was called (topic may be null in test due to @Value not being injected)
        verify(kafkaTemplate).send(nullable(String.class), eq("m-1"), eq(event));
    }

    @Test
    void logsErrorWhenKafkaSendFails() {
        // Arrange: mock kafkaTemplate to return a failed future
        CompletableFuture<SendResult<String, MediaEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka is down"));
        when(kafkaTemplate.send(nullable(String.class), anyString(), any(MediaEvent.class)))
                .thenReturn(future);

        MediaEvent event = new MediaEvent();
        event.setMediaId("m-2");
        event.setEventType(IMAGE_DELETED);

        // Act: should not throw - error is logged
        producer.sendMediaEvent(event);

        // Assert: verify send was still called (topic may be null in test due to @Value not being injected)
        verify(kafkaTemplate).send(nullable(String.class), eq("m-2"), eq(event));
    }
}
