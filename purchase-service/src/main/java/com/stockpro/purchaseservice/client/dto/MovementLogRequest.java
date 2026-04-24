package com.stockpro.purchaseservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MovementLogRequest {
    private Long warehouseId;
    private Long productId;
    private String movementType;
    private Integer quantity;
    private Long referenceId;
    private String referenceType;
    private String notes;
    private Long performedBy;
}
