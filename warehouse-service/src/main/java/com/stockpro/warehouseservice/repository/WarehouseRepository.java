package com.stockpro.warehouseservice.repository;

import com.stockpro.warehouseservice.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByName(String name);

    boolean existsByName(String name);

    List<Warehouse> findByIsActive(Boolean isActive);
}