package com.stockpro.reportservice.client;

import com.stockpro.reportservice.dto.ProductDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/api/v1/products")
    List<ProductDto> getAllProducts(@RequestHeader("Authorization") String authHeader);
}
