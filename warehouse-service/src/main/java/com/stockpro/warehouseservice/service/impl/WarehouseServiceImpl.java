package com.stockpro.warehouseservice.service.impl;

import com.stockpro.warehouseservice.client.MovementServiceClient;
import com.stockpro.warehouseservice.dto.*;
import com.stockpro.warehouseservice.entity.*;
import com.stockpro.warehouseservice.event.StockEventPublisher;
import com.stockpro.warehouseservice.exception.*;
import com.stockpro.warehouseservice.repository.*;
import com.stockpro.warehouseservice.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StockLevelRepository stockLevelRepository;
    private final StockEventPublisher stockEventPublisher;
    private final MovementServiceClient movementServiceClient;

    // ─── Warehouse CRUD ───────────────────────────────────────────────────────

    @Override
    public WarehouseResponse createWarehouse(WarehouseRequest request) {
        if (warehouseRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Warehouse already exists with name: " + request.getName());
        }
        com.stockpro.warehouseservice.entity.Warehouse warehouse = com.stockpro.warehouseservice.entity.Warehouse.builder()
                .name(request.getName())
                .location(request.getLocation())
                .capacity(request.getCapacity())
                .isActive(true)
                .build();
        return mapToWarehouseResponse(warehouseRepository.save(warehouse));
    }

    @Override
    public WarehouseResponse getWarehouseById(Long id) {
        return mapToWarehouseResponse(findWarehouseOrThrow(id));
    }

    @Override
    public List<WarehouseResponse> getAllWarehouses() {
        return warehouseRepository.findAll()
                .stream().map(this::mapToWarehouseResponse).collect(Collectors.toList());
    }

    @Override
    public List<WarehouseResponse> getActiveWarehouses() {
        return warehouseRepository.findByIsActive(true)
                .stream().map(this::mapToWarehouseResponse).collect(Collectors.toList());
    }

    @Override
    public WarehouseResponse updateWarehouse(Long id, WarehouseRequest request) {
        com.stockpro.warehouseservice.entity.Warehouse warehouse = findWarehouseOrThrow(id);
        if (!warehouse.getName().equals(request.getName())
                && warehouseRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Warehouse name already in use: " + request.getName());
        }
        warehouse.setName(request.getName());
        warehouse.setLocation(request.getLocation());
        warehouse.setCapacity(request.getCapacity());
        return mapToWarehouseResponse(warehouseRepository.save(warehouse));
    }

    @Override
    public void activateWarehouse(Long id) {
        com.stockpro.warehouseservice.entity.Warehouse warehouse = findWarehouseOrThrow(id);
        warehouse.setIsActive(true);
        warehouseRepository.save(warehouse);
    }

    @Override
    public void deactivateWarehouse(Long id) {
        com.stockpro.warehouseservice.entity.Warehouse warehouse = findWarehouseOrThrow(id);
        warehouse.setIsActive(false);
        warehouseRepository.save(warehouse);
    }

    // ─── Stock Operations ─────────────────────────────────────────────────────

    @Override
    public StockLevelResponse getStockLevel(Long warehouseId, Long productId) {
        com.stockpro.warehouseservice.entity.StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No stock record found for warehouseId=" + warehouseId + ", productId=" + productId));
        return mapToStockResponse(stock);
    }

    @Override
    public List<StockLevelResponse> getStockByWarehouse(Long warehouseId) {
        findWarehouseOrThrow(warehouseId);
        return stockLevelRepository.findByWarehouseId(warehouseId)
                .stream().map(this::mapToStockResponse).collect(Collectors.toList());
    }

    @Override
    public List<StockLevelResponse> getStockByProduct(Long productId) {
        return stockLevelRepository.findByProductId(productId)
                .stream().map(this::mapToStockResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StockLevelResponse addStock(StockUpdateRequest request) {
        if (request.getQuantity() <= 0) {
            throw new RuntimeException("Quantity to add must be positive.");
        }
        findWarehouseOrThrow(request.getWarehouseId());

        com.stockpro.warehouseservice.entity.StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(request.getWarehouseId(), request.getProductId())
                .orElseGet(() -> com.stockpro.warehouseservice.entity.StockLevel.builder()
                        .warehouseId(request.getWarehouseId())
                        .productId(request.getProductId())
                        .quantity(0)
                        .build());

        stock.setQuantity(stock.getQuantity() + request.getQuantity());
        com.stockpro.warehouseservice.entity.StockLevel saved = stockLevelRepository.save(stock);
        logMovement(saved.getWarehouseId(), saved.getProductId(),
                "STOCK_IN", request.getQuantity(),
                "Manual stock addition to warehouse " + saved.getWarehouseId());
        return mapToStockResponse(saved);
    }

    @Override
    @Transactional
    public StockLevelResponse deductStock(StockUpdateRequest request) {
        if (request.getQuantity() <= 0) {
            throw new RuntimeException("Quantity to deduct must be positive.");
        }
        findWarehouseOrThrow(request.getWarehouseId());

        com.stockpro.warehouseservice.entity.StockLevel stock = stockLevelRepository
                .findByWarehouseIdAndProductId(request.getWarehouseId(), request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No stock found for warehouseId=" + request.getWarehouseId()
                                + ", productId=" + request.getProductId()));

        if (stock.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock. Available: " + stock.getQuantity()
                            + ", Requested: " + request.getQuantity());
        }

        stock.setQuantity(stock.getQuantity() - request.getQuantity());

        com.stockpro.warehouseservice.entity.StockLevel saved = stockLevelRepository.save(stock);
        stockEventPublisher.publishIfLowStock(
                saved.getWarehouseId(), saved.getProductId(), saved.getQuantity());

        logMovement(saved.getWarehouseId(), saved.getProductId(),
                "STOCK_OUT", request.getQuantity(),
                "Manual deduction from warehouse " + saved.getWarehouseId());
        return mapToStockResponse(saved);
    }

    @Override
    public List<StockLevelResponse> getAllStock() {
        return stockLevelRepository.findAll()
                .stream().map(this::mapToStockResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void transferStock(StockTransferRequest request) {
        if (request.getQuantity() <= 0) {
            throw new RuntimeException("Transfer quantity must be positive.");
        }
        if (request.getFromWarehouseId().equals(request.getToWarehouseId())) {
            throw new RuntimeException("Source and destination warehouses must be different.");
        }

        findWarehouseOrThrow(request.getFromWarehouseId());
        findWarehouseOrThrow(request.getToWarehouseId());

        // Deduct from source — @Version will catch concurrent modification
        com.stockpro.warehouseservice.entity.StockLevel source = stockLevelRepository
                .findByWarehouseIdAndProductId(request.getFromWarehouseId(), request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No stock found in source warehouse for this product."));

        if (source.getQuantity() < request.getQuantity()) {
            throw new InsufficientStockException(
                    "Insufficient stock in source warehouse. Available: " + source.getQuantity()
                            + ", Requested: " + request.getQuantity());
        }
        source.setQuantity(source.getQuantity() - request.getQuantity());


        // Add to destination — create record if it doesn't exist
        com.stockpro.warehouseservice.entity.StockLevel destination = stockLevelRepository
                .findByWarehouseIdAndProductId(request.getToWarehouseId(), request.getProductId())
                .orElseGet(() -> com.stockpro.warehouseservice.entity.StockLevel.builder()
                        .warehouseId(request.getToWarehouseId())
                        .productId(request.getProductId())
                        .quantity(0)
                        .build());

        destination.setQuantity(destination.getQuantity() + request.getQuantity());
        stockLevelRepository.save(source);
        stockLevelRepository.save(destination);
        stockEventPublisher.publishIfLowStock(
                source.getWarehouseId(), source.getProductId(), source.getQuantity());

        logMovement(request.getFromWarehouseId(), request.getProductId(),
                "STOCK_OUT", request.getQuantity(),
                "Transfer OUT to warehouse " + request.getToWarehouseId());

        logMovement(request.getToWarehouseId(), request.getProductId(),
                "STOCK_IN", request.getQuantity(),
                "Transfer IN from warehouse " + request.getFromWarehouseId());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private com.stockpro.warehouseservice.entity.Warehouse findWarehouseOrThrow(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found with id: " + id));
    }

    private WarehouseResponse mapToWarehouseResponse(com.stockpro.warehouseservice.entity.Warehouse w) {
        return WarehouseResponse.builder()
                .id(w.getId())
                .name(w.getName())
                .location(w.getLocation())
                .capacity(w.getCapacity())
                .isActive(w.getIsActive())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }

    private StockLevelResponse mapToStockResponse(com.stockpro.warehouseservice.entity.StockLevel s) {
        return StockLevelResponse.builder()
                .id(s.getId())
                .warehouseId(s.getWarehouseId())
                .productId(s.getProductId())
                .quantity(s.getQuantity())
                .version(s.getVersion())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    // Add inside WarehouseServiceImpl — after all methods, before closing brace
    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class MovementLogRequest {
        private Long warehouseId;
        private Long productId;
        private String movementType;
        private Integer quantity;
        private Long referenceId;
        private String referenceType;
        private String notes;
        private Long performedBy;
    }

    private void logMovement(Long warehouseId, Long productId,
                             String movementType, Integer quantity, String notes) {
        try {
            MovementLogRequest req = new MovementLogRequest(
                    warehouseId, productId, movementType, quantity,
                    null, "MANUAL", notes, 1L
            );
            movementServiceClient.createMovement(req);
        } catch (Exception e) {
            // Best-effort — never fail stock operation because of movement logging
            log.error("Movement log failed for warehouseId={}, productId={}: {}",
                    warehouseId, productId, e.getMessage());
        }
    }
}