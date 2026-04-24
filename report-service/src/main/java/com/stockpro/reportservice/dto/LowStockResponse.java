package com.stockpro.reportservice.dto;

import com.stockpro.reportservice.dto.LowStockItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class LowStockResponse {
    private int totalLowStockProducts;
    private List<LowStockItem> items;
}