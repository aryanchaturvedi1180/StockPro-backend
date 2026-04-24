package com.stockpro.purchaseservice.repository;

import com.stockpro.purchaseservice.entity.PoStatus;
import com.stockpro.purchaseservice.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    List<PurchaseOrder> findByStatus(PoStatus status);

    List<PurchaseOrder> findBySupplierId(Long supplierId);

    List<PurchaseOrder> findByOrderedBy(Long userId);
}