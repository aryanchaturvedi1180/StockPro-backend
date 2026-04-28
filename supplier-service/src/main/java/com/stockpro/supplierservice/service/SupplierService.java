package com.stockpro.supplierservice.service;

import com.stockpro.supplierservice.dto.SupplierRequest;
import com.stockpro.supplierservice.dto.SupplierResponse;

import java.util.List;

public interface SupplierService {
    SupplierResponse create(SupplierRequest request);

    SupplierResponse getById(Long id);

    List<SupplierResponse> getAll();

    List<SupplierResponse> getAllActive();

    SupplierResponse update(Long id, SupplierRequest request);

    SupplierResponse activate(Long id);

    SupplierResponse deactivate(Long id);

    void delete(Long id);
}
