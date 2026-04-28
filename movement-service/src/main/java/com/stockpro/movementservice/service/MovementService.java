package com.stockpro.movementservice.service;

import com.stockpro.movementservice.dto.*;
import com.stockpro.movementservice.entity.MovementType;
import java.time.LocalDateTime;
import java.util.List;

public interface MovementService {
    MovementResponse logMovement(MovementRequest request);

    List<MovementResponse> logTransfer(TransferMovementRequest request);

    MovementResponse getById(Long id);

    List<MovementResponse> getAll();
    List<MovementResponse> getByWarehouse(Long warehouseId);

    List<MovementResponse> getByProduct(Long productId);


    List<MovementResponse> getByType(MovementType type);

    List<MovementResponse> getByPerformedBy(Long userId);

    List<MovementResponse> getByReference(Long referenceId, String referenceType);

    List<MovementResponse> getByDateRange(LocalDateTime from, LocalDateTime to);

    List<MovementResponse> getByWarehouseAndProduct(Long warehouseId, Long productId);
}