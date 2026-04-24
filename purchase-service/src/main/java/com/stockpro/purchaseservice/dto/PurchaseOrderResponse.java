package com.stockpro.purchaseservice.dto;

import com.stockpro.purchaseservice.entity.PoStatus;
import com.stockpro.purchaseservice.dto.PurchaseOrderItemResponse;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponse {

    private Long id;
    private Long supplierId;
    private PoStatus status;
    private Long orderedBy;
    private Long approvedBy;
    private Long receivedBy;
    private String notes;
    private LocalDate expectedDate;
    private LocalDateTime receivedDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<PurchaseOrderItemResponse> items;
}