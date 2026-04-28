package com.stockpro.reportservice.dto;

import com.stockpro.reportservice.dto.ProductStockValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class StockValueResponse {

    private BigDecimal totalStockValue;
    private int totalProducts;
    private int totalUnits;
    private List<ProductStockValue> breakdown;
}