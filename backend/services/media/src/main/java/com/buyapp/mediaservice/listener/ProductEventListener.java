package com.buyapp.mediaservice.listener;

import com.buyapp.common.event.ProductEvent;
import com.buyapp.mediaservice.service.MediaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductEventListener.class);

    private final MediaService mediaService;

    public ProductEventListener(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @KafkaListener(topics = "${kafka.topic.product-events:product-events}",
    groupId = "${spring.kafka.consumer.group-id:media-service-group}",
    containerFactory = "productEventKafkaListenerContainerFactory")
    
    public void handleProductEvent(ProductEvent event) {
        log.info("Received product event: {}", event);

        try {
            switch (event.getEventType()) {
                case PRODUCT_CREATED:
                    log.info("Product created: {} - Ready to accept media uploads", event.getProductId());
                    // Future: Pre-create media directories or prepare storage
                    break;

                case PRODUCT_DELETED:
                    log.info("Product deleted: {} - Cleaning up associated media", event.getProductId());
                    // Delete all media associated with this product
                    mediaService.deleteMediaByProductIdInternal(event.getProductId());
                    break;

                case PRODUCT_UPDATED:
                    log.info("Product updated: {} - No media action required", event.getProductId());
                    // Future: Update metadata or thumbnails if needed
                    break;

                default:
                    log.warn("Unknown product event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Error processing product event: {}", e.getMessage(), e);
            // Future: Implement retry logic or dead letter queue
        }
    }
}
