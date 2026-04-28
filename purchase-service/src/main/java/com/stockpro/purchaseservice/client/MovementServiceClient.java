package com.stockpro.purchaseservice.client;

import com.stockpro.purchaseservice.client.dto.MovementLogRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "movement-service", url = "${service.movement.url}")
public interface MovementServiceClient {

    @PostMapping("/api/v1/movements")
    ResponseEntity<Object> createMovement(@RequestBody MovementLogRequest request);
}
