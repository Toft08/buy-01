package com.buyapp.mediaservice.listener;

import com.buyapp.common.event.ProductEvent;
import com.buyapp.mediaservice.service.MediaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;
import static com.buyapp.common.event.ProductEvent.EventType.*;

@ExtendWith(MockitoExtension.class)
class ProductEventListenerTest {

    @Mock
    private MediaService mediaService;

    private ProductEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ProductEventListener(mediaService);
    }

    @Test
    void whenProductDeleted_thenDeletesMedia() {
        // Arrange: create a PRODUCT_DELETED event
        ProductEvent event = new ProductEvent();
        event.setEventType(PRODUCT_DELETED);
        event.setProductId("prod-123");

        // Act: call handler directly
        listener.handleProductEvent(event);

        // Assert: verify the mediaService was asked to delete the media for that
        // product
        verify(mediaService, times(1)).deleteMediaByProductIdInternal("prod-123");
    }

    @Test
    void whenProductCreated_thenNoMediaDeletion() {
        // Arrange
        ProductEvent event = new ProductEvent();
        event.setEventType(PRODUCT_CREATED);
        event.setProductId("prod-456");

        // Act
        listener.handleProductEvent(event);

        // Assert: verify no deletion happened
        verify(mediaService, never()).deleteMediaByProductIdInternal(anyString());
    }

    @Test
    void whenProductUpdated_thenNoMediaDeletion() {
        // Arrange
        ProductEvent event = new ProductEvent();
        event.setEventType(PRODUCT_UPDATED);
        event.setProductId("prod-789");

        // Act
        listener.handleProductEvent(event);

        // Assert: verify no deletion happened
        verify(mediaService, never()).deleteMediaByProductIdInternal(anyString());
    }

    @Test
    void whenProductDeletedAndServiceThrows_thenErrorIsLogged() {
        // Arrange
        ProductEvent event = new ProductEvent();
        event.setEventType(PRODUCT_DELETED);
        event.setProductId("prod-error");

        doThrow(new RuntimeException("Media deletion failed"))
                .when(mediaService).deleteMediaByProductIdInternal("prod-error");

        // Act: should not throw - error is caught and logged
        listener.handleProductEvent(event);

        // Assert: verify the service was still called despite the error
        verify(mediaService, times(1)).deleteMediaByProductIdInternal("prod-error");
    }
}
