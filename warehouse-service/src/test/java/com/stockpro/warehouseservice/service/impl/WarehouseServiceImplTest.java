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
import com.stockpro.warehouseservice.exception.ResourceNotFoundException;
import com.stockpro.warehouseservice.repository.StockLevelRepository;
import com.stockpro.warehouseservice.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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

    @Test
    void createWarehouseShouldSaveWhenNameIsUnique() {
        WarehouseRequest request = warehouseRequest("Noida Warehouse");

        when(warehouseRepository.existsByName("Noida Warehouse")).thenReturn(false);
        when(warehouseRepository.save(any(Warehouse.class)))
                .thenReturn(warehouse(1L, "Noida Warehouse", true));

        var response = warehouseService.createWarehouse(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Noida Warehouse");
        assertThat(response.getIsActive()).isTrue();
    }

    @Test
    void createWarehouseShouldThrowWhenNameExists() {
        WarehouseRequest request = warehouseRequest("Noida Warehouse");

        when(warehouseRepository.existsByName("Noida Warehouse")).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.createWarehouse(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Warehouse already exists");

        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void getWarehouseByIdShouldReturnWarehouse() {
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(warehouse(1L, "Delhi Warehouse", true)));

        var response = warehouseService.getWarehouseById(1L);

        assertThat(response.getName()).isEqualTo("Delhi Warehouse");
    }

    @Test
    void getWarehouseByIdShouldThrowWhenMissing() {
        when(warehouseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> warehouseService.getWarehouseById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Warehouse not found");
    }

    @Test
    void getAllWarehousesShouldReturnList() {
        when(warehouseRepository.findAll()).thenReturn(List.of(
                warehouse(1L, "Noida", true),
                warehouse(2L, "Delhi", true)
        ));

        var response = warehouseService.getAllWarehouses();

        assertThat(response).hasSize(2);
    }

    @Test
    void getActiveWarehousesShouldReturnActiveList() {
        when(warehouseRepository.findByIsActive(true)).thenReturn(List.of(
                warehouse(1L, "Noida", true)
        ));

        var response = warehouseService.getActiveWarehouses();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getIsActive()).isTrue();
    }

    @Test
    void updateWarehouseShouldUpdateWhenNameIsSame() {
        Warehouse existing = warehouse(1L, "Noida", true);
        WarehouseRequest request = warehouseRequest("Noida");
        request.setLocation("Updated Location");
        request.setCapacity(200);

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.save(existing)).thenReturn(existing);

        var response = warehouseService.updateWarehouse(1L, request);

        assertThat(response.getLocation()).isEqualTo("Updated Location");
        assertThat(response.getCapacity()).isEqualTo(200);
        verify(warehouseRepository, never()).existsByName("Noida");
    }

    @Test
    void updateWarehouseShouldThrowWhenNewNameAlreadyExists() {
        Warehouse existing = warehouse(1L, "Noida", true);
        WarehouseRequest request = warehouseRequest("Delhi");

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(warehouseRepository.existsByName("Delhi")).thenReturn(true);

        assertThatThrownBy(() -> warehouseService.updateWarehouse(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Warehouse name already in use");
    }

    @Test
    void activateWarehouseShouldSetActiveTrue() {
        Warehouse existing = warehouse(1L, "Noida", false);

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existing));

        warehouseService.activateWarehouse(1L);

        assertThat(existing.getIsActive()).isTrue();
        verify(warehouseRepository).save(existing);
    }

    @Test
    void deactivateWarehouseShouldSetActiveFalse() {
        Warehouse existing = warehouse(1L, "Noida", true);

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(existing));

        warehouseService.deactivateWarehouse(1L);

        assertThat(existing.getIsActive()).isFalse();
        verify(warehouseRepository).save(existing);
    }

    @Test
    void addStockShouldCreateNewStockWhenRecordMissing() {
        StockUpdateRequest request = stockUpdateRequest(1L, 10L, 50);

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "Noida", true)));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 10L)).thenReturn(Optional.empty());
        when(stockLevelRepository.save(any(StockLevel.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = warehouseService.addStock(request);

        assertThat(response.getQuantity()).isEqualTo(50);
        verify(movementServiceClient).createMovement(any());
    }

    @Test
    void addStockShouldThrowWhenQuantityInvalid() {
        StockUpdateRequest request = stockUpdateRequest(1L, 10L, 0);

        assertThatThrownBy(() -> warehouseService.addStock(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity to add must be positive");
    }

    @Test
    void deductStockShouldReduceStockAndPublishEvent() {
        StockUpdateRequest request = stockUpdateRequest(1L, 10L, 20);
        StockLevel stock = stockLevel(1L, 10L, 100);

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "Noida", true)));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 10L)).thenReturn(Optional.of(stock));
        when(stockLevelRepository.save(stock)).thenReturn(stock);

        var response = warehouseService.deductStock(request);

        assertThat(response.getQuantity()).isEqualTo(80);
        verify(stockEventPublisher).publishIfLowStock(1L, 10L, 80);
        verify(movementServiceClient).createMovement(any());
    }

    @Test
    void deductStockShouldThrowWhenInsufficientStock() {
        StockUpdateRequest request = stockUpdateRequest(1L, 10L, 200);
        StockLevel stock = stockLevel(1L, 10L, 50);

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "Noida", true)));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 10L)).thenReturn(Optional.of(stock));

        assertThatThrownBy(() -> warehouseService.deductStock(request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    void getStockLevelShouldReturnStock() {
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 10L))
                .thenReturn(Optional.of(stockLevel(1L, 10L, 80)));

        var response = warehouseService.getStockLevel(1L, 10L);

        assertThat(response.getQuantity()).isEqualTo(80);
    }

    @Test
    void getStockByWarehouseShouldReturnStockList() {
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "Noida", true)));
        when(stockLevelRepository.findByWarehouseId(1L)).thenReturn(List.of(stockLevel(1L, 10L, 80)));

        var response = warehouseService.getStockByWarehouse(1L);

        assertThat(response).hasSize(1);
    }

    @Test
    void getStockByProductShouldReturnStockList() {
        when(stockLevelRepository.findByProductId(10L)).thenReturn(List.of(stockLevel(1L, 10L, 80)));

        var response = warehouseService.getStockByProduct(10L);

        assertThat(response).hasSize(1);
    }

    @Test
    void transferStockShouldMoveQuantityBetweenWarehouses() {
        StockTransferRequest request = new StockTransferRequest();
        request.setFromWarehouseId(1L);
        request.setToWarehouseId(2L);
        request.setProductId(10L);
        request.setQuantity(30);

        StockLevel source = stockLevel(1L, 10L, 100);
        StockLevel destination = stockLevel(2L, 10L, 20);

        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(warehouse(1L, "Source", true)));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse(2L, "Destination", true)));
        when(stockLevelRepository.findByWarehouseIdAndProductId(1L, 10L)).thenReturn(Optional.of(source));
        when(stockLevelRepository.findByWarehouseIdAndProductId(2L, 10L)).thenReturn(Optional.of(destination));

        warehouseService.transferStock(request);

        assertThat(source.getQuantity()).isEqualTo(70);
        assertThat(destination.getQuantity()).isEqualTo(50);
        verify(stockLevelRepository, times(2)).save(any(StockLevel.class));
        verify(stockEventPublisher).publishIfLowStock(1L, 10L, 70);
    }

    @Test
    void transferStockShouldThrowWhenSameWarehouse() {
        StockTransferRequest request = new StockTransferRequest();
        request.setFromWarehouseId(1L);
        request.setToWarehouseId(1L);
        request.setProductId(10L);
        request.setQuantity(10);

        assertThatThrownBy(() -> warehouseService.transferStock(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source and destination warehouses must be different");
    }

    private WarehouseRequest warehouseRequest(String name) {
        return WarehouseRequest.builder()
                .name(name)
                .location("Noida")
                .capacity(100)
                .build();
    }

    private Warehouse warehouse(Long id, String name, boolean active) {
        return Warehouse.builder()
                .id(id)
                .name(name)
                .location("Noida")
                .capacity(100)
                .isActive(active)
                .build();
    }

    private StockUpdateRequest stockUpdateRequest(Long warehouseId, Long productId, Integer quantity) {
        StockUpdateRequest request = new StockUpdateRequest();
        request.setWarehouseId(warehouseId);
        request.setProductId(productId);
        request.setQuantity(quantity);
        return request;
    }

    private StockLevel stockLevel(Long warehouseId, Long productId, Integer quantity) {
        return StockLevel.builder()
                .id(1L)
                .warehouseId(warehouseId)
                .productId(productId)
                .quantity(quantity)
                .version(0L)
                .build();
    }
}