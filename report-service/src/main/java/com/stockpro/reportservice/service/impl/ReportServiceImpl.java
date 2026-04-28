package com.stockpro.reportservice.service.impl;

import com.stockpro.reportservice.client.ProductServiceClient;
import com.stockpro.reportservice.client.WarehouseServiceClient;
import com.stockpro.reportservice.dto.*;
import com.stockpro.reportservice.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ProductServiceClient productServiceClient;
    private final WarehouseServiceClient warehouseServiceClient;

    @Override
    public StockValueResponse getTotalStockValue(String authHeader) {

        // Step 1 — get all products from product-service
        List<ProductDto> products = getAllProducts(authHeader);

        // Step 2 — get all stock levels from warehouse-service
        List<StockLevelDto> stockLevels = getAllStockLevels(authHeader);

        // Step 3 — build product price map for quick lookup
        Map<Long, ProductDto> productMap = products.stream()
                .collect(Collectors.toMap(ProductDto::getId, p -> p));

        // Step 4 — calculate value per product
        List<ProductStockValue> breakdown = new ArrayList<>();
        BigDecimal totalValue = BigDecimal.ZERO;
        int totalUnits = 0;

        for (StockLevelDto stock : stockLevels) {
            ProductDto product = productMap.get(stock.getProductId());
            if (product == null || product.getPrice() == null) continue;

            BigDecimal itemValue = product.getPrice()
                    .multiply(BigDecimal.valueOf(stock.getQuantity()));

            breakdown.add(new ProductStockValue(
                    product.getId(),
                    product.getName(),
                    product.getSku(),
                    stock.getQuantity(),
                    product.getPrice(),
                    itemValue
            ));

            totalValue = totalValue.add(itemValue);
            totalUnits += stock.getQuantity();
        }

        return new StockValueResponse(
                totalValue,
                breakdown.size(),
                totalUnits,
                breakdown
        );
    }

    @Override
    public LowStockResponse getLowStockProducts(String authHeader) {

        // Step 1 — get all products
        List<ProductDto> products = getAllProducts(authHeader);

        // Step 2 — get all stock levels
        List<StockLevelDto> stockLevels = getAllStockLevels(authHeader);

        // Step 3 — build product map
        Map<Long, ProductDto> productMap = products.stream()
                .collect(Collectors.toMap(ProductDto::getId, p -> p));

        // Step 4 — find products below reorder level
        List<LowStockItem> lowStockItems = new ArrayList<>();

        for (StockLevelDto stock : stockLevels) {
            ProductDto product = productMap.get(stock.getProductId());
            if (product == null) continue;

            if (stock.getQuantity() < product.getReorderLevel()) {
                int shortage = product.getReorderLevel() - stock.getQuantity();
                lowStockItems.add(new LowStockItem(
                        product.getId(),
                        product.getName(),
                        product.getSku(),
                        stock.getQuantity(),
                        product.getReorderLevel(),
                        shortage
                ));
            }
        }

        return new LowStockResponse(lowStockItems.size(), lowStockItems);
    }

    private List<ProductDto> getAllProducts(String authHeader) {
        try {
            List<ProductDto> response = productServiceClient.getAllProducts(authHeader);
            return response != null ? response : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to fetch products: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<StockLevelDto> getAllStockLevels(String authHeader) {
        try {
            List<StockLevelDto> response = warehouseServiceClient.getAllStockLevels(authHeader);
            return response != null ? response : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to fetch stock levels: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
