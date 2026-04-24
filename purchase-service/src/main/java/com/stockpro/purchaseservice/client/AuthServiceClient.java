package com.stockpro.purchaseservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "auth-service", url = "${service.auth.url}")
public interface AuthServiceClient {

    @GetMapping("/api/v1/auth/validate")
    ResponseEntity<Void> validateToken(@RequestParam("token") String token);
}
