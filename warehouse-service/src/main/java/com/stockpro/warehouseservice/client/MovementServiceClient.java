package com.stockpro.warehouseservice.client;

import com.stockpro.warehouseservice.service.impl.WarehouseServiceImpl.MovementLogRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "movement-service",
        url = "${service.movement.url}",
        configuration = com.stockpro.warehouseservice.config.FeignClientConfig.class
)
public interface MovementServiceClient {

    @PostMapping("/api/v1/movements")
    Object createMovement(@RequestBody MovementLogRequest request);
}
