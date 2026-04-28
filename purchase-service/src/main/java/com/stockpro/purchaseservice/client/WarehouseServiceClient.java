package com.stockpro.purchaseservice.client;

import com.stockpro.purchaseservice.client.dto.StockUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "warehouse-service", url = "${service.warehouse.url}")
public interface WarehouseServiceClient {

    @PostMapping("/api/v1/warehouses/stock/add")
    ResponseEntity<Object> addStock(@RequestBody StockUpdateRequest request);
}
