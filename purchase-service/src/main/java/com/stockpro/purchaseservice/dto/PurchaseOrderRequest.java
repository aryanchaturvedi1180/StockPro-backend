package com.stockpro.purchaseservice.dto;

import com.stockpro.purchaseservice.dto.PurchaseOrderItemRequest;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderRequest {

    private Long supplierId;
    private Long orderedBy;
    private String notes;
    private LocalDate expectedDate;
    private List<PurchaseOrderItemRequest> items;
}