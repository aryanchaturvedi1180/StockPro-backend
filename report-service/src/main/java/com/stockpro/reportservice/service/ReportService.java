package com.stockpro.reportservice.service;

import com.stockpro.reportservice.dto.LowStockResponse;
import com.stockpro.reportservice.dto.StockValueResponse;

public interface ReportService {

    StockValueResponse getTotalStockValue(String authHeader);

    LowStockResponse getLowStockProducts(String authHeader);
}