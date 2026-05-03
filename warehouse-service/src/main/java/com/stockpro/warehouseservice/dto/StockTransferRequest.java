package com.stockpro.warehouseservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockTransferRequest {
    @NotNull(message = "Source warehouse id is required")
    private Long fromWarehouseId;
    @NotNull(message = "Destination warehouse id is required")
    private Long toWarehouseId;
    @NotNull(message = "Product id is required")
    private Long productId;
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be positive")
    private Integer quantity;
}