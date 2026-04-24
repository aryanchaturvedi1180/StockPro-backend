package com.stockpro.reportservice.client;

import com.stockpro.reportservice.dto.StockLevelDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "warehouse-service")
public interface WarehouseServiceClient {

    @GetMapping("/api/v1/warehouses/stock")
    List<StockLevelDto> getAllStockLevels(@RequestHeader("Authorization") String authHeader);
}
