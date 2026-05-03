package com.stockpro.warehouseservice.service.impl;

import com.stockpro.warehouseservice.client.MovementServiceClient;
import com.stockpro.warehouseservice.dto.StockTransferRequest;
import com.stockpro.warehouseservice.dto.StockUpdateRequest;
import com.stockpro.warehouseservice.dto.WarehouseRequest;
import com.stockpro.warehouseservice.entity.StockLevel;
import com.stockpro.warehouseservice.entity.Warehouse;
import com.stockpro.warehouseservice.event.StockEventPublisher;
import com.stockpro.warehouseservice.exception.DuplicateResourceException;
import com.stockpro.warehouseservice.exception.InsufficientStockException;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceImplTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private StockLevelRepository stockLevelRepository;

    @Mock
    private StockEventPublisher stockEventPublisher;

    @Mock
    private MovementServiceClient movementServiceClient;

    @InjectMocks
    private WarehouseServiceImpl warehouseService;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = Warehouse.builder().id(1L).name("Noida Warehouse").location("Noida").capacity(1000).isActive(true).build();
    }

    @Test
    void createWarehouseShouldRejectDuplicateName() {
        WarehouseRequest request = WarehouseRequest.builder().name("Noida Warehouse").location("Noida").capacity(1000).build();
        when(warehouseRepository.existsByName("Noida Warehouse")).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.createWarehouse(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Warehouse already exists");
    }

    @Test
    void addStockShouldCreateStockLevelWhenMissing() {
        StockUpdateRequest request = StockUpdateRequest.builder().warehouseId(1L).productId(10L).quantity(15).build();
        StockLevel saved = StockLevel.builder().id(2L).warehouseId(1L).productId(10L).quantity(15).build();

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 10L)).thenReturn(Optional.empty());
        when(stockLevelRepository.save(any(StockLevel.class))).thenReturn(saved);

        var response = warehouseService.addStock(request);

        assertThat(response.getQuantity()).isEqualTo(15);
        verify(stockLevelRepository).save(any(StockLevel.class));
    }

    @Test
    void deductStockShouldFailWhenInsufficientQuantity() {
        StockUpdateRequest request = StockUpdateRequest.builder().warehouseId(1L).productId(10L).quantity(20).build();
        StockLevel stock = StockLevel.builder().id(2L).warehouseId(1L).productId(10L).quantity(5).build();

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 10L)).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> warehouseService.deductStock(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void transferStockShouldMoveQuantityBetweenWarehouses() {
        StockTransferRequest request = StockTransferRequest.builder()
                .fromWarehouseId(1L).toWarehouseId(2L).productId(10L).quantity(30).build();
        Warehouse destinationWarehouse = Warehouse.builder().id(2L).name("Delhi Warehouse").location("Delhi").capacity(800).isActive(true).build();
        StockLevel source = StockLevel.builder().warehouseId(1L).productId(10L).quantity(100).build();
        StockLevel destination = StockLevel.builder().warehouseId(2L).productId(10L).quantity(5).build();

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(destinationWarehouse));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 10L)).thenReturn(Optional.of(source));
        when(stockLevelRepository.findByWarehouseIdAndProductId(2L, 10L)).thenReturn(Optional.of(destination));

        warehouseService.transferStock(request);

        assertThat(source.getQuantity()).isEqualTo(70);
        assertThat(destination.getQuantity()).isEqualTo(35);
        verify(stockLevelRepository, times(2)).save(any(StockLevel.class));
    }
}
