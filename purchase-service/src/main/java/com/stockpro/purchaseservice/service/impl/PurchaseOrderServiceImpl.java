package com.stockpro.purchaseservice.service.impl;

import com.stockpro.purchaseservice.client.MovementServiceClient;
import com.stockpro.purchaseservice.client.WarehouseServiceClient;
import com.stockpro.purchaseservice.client.dto.MovementLogRequest;
import com.stockpro.purchaseservice.client.dto.StockUpdateRequest;
import com.stockpro.purchaseservice.dto.*;
import com.stockpro.purchaseservice.entity.*;
import com.stockpro.purchaseservice.event.PurchaseEventPublisher;
import com.stockpro.purchaseservice.exception.*;
import com.stockpro.purchaseservice.repository.*;
import com.stockpro.purchaseservice.service.PurchaseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

    private final PurchaseOrderRepository poRepository;
    private final PurchaseOrderItemRepository itemRepository;
    private final WarehouseServiceClient warehouseServiceClient;
    private final MovementServiceClient movementServiceClient;
    private final PurchaseEventPublisher eventPublisher;

    @Override
    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("Purchase order must have at least one item.");
        }

        PurchaseOrder po = PurchaseOrder.builder()
                .supplierId(request.getSupplierId())
                .orderedBy(request.getOrderedBy())
                .notes(request.getNotes())
                .expectedDate(request.getExpectedDate())
                .status(PoStatus.DRAFT)
                .build();

        List<PurchaseOrderItem> items = request.getItems().stream()
                .map(itemReq -> PurchaseOrderItem.builder()
                        .purchaseOrder(po)
                        .productId(itemReq.getProductId())
                        .warehouseId(itemReq.getWarehouseId())
                        .quantityOrdered(itemReq.getQuantityOrdered())
                        .quantityReceived(0)
                        .unitPrice(itemReq.getUnitPrice())
                        .build())
                .collect(Collectors.toList());

        po.setItems(items);
        return mapToResponse(poRepository.save(po));
    }

    @Override
    public PurchaseOrderResponse getPurchaseOrderById(Long id) {
        return mapToResponse(findPoOrThrow(id));
    }

    @Override
    public List<PurchaseOrderResponse> getAllPurchaseOrders() {
        return poRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchaseOrderResponse> getByStatus(PoStatus status) {
        return poRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PurchaseOrderResponse> getBySupplier(Long supplierId) {
        return poRepository.findBySupplierId(supplierId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PurchaseOrderResponse approvePurchaseOrder(Long id, ApproveRequest request) {
        PurchaseOrder po = findPoOrThrow(id);

        if (po.getStatus() != PoStatus.DRAFT) {
            throw new InvalidStatusTransitionException(
                    "Only DRAFT orders can be approved. Current status: " + po.getStatus());
        }

        po.setStatus(PoStatus.APPROVED);
        po.setApprovedBy(request.getApprovedBy());
        PurchaseOrder saved = poRepository.save(po);

        if (saved.getExpectedDate() != null && saved.getExpectedDate().isBefore(LocalDate.now())) {
            eventPublisher.publishPoOverdue(saved.getId(), saved.getSupplierId(), saved.getExpectedDate());
        }

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public PurchaseOrderResponse cancelPurchaseOrder(Long id) {
        PurchaseOrder po = findPoOrThrow(id);

        if (po.getStatus() == PoStatus.RECEIVED) {
            throw new InvalidStatusTransitionException("Cannot cancel a RECEIVED order.");
        }
        if (po.getStatus() == PoStatus.CANCELLED) {
            throw new InvalidStatusTransitionException("Order is already CANCELLED.");
        }

        po.setStatus(PoStatus.CANCELLED);
        return mapToResponse(poRepository.save(po));
    }

    @Override
    @Transactional
    public PurchaseOrderResponse receiveGoods(Long id, ReceiveGoodsRequest request) {
        PurchaseOrder po = findPoOrThrow(id);

        if (po.getStatus() != PoStatus.APPROVED) {
            throw new InvalidStatusTransitionException(
                    "Only APPROVED orders can be received. Current status: " + po.getStatus());
        }

        for (PurchaseOrderItem item : po.getItems()) {
            StockUpdateRequest stockRequest = new StockUpdateRequest(
                    item.getWarehouseId(),
                    item.getProductId(),
                    item.getQuantityOrdered()
            );

            try {
                warehouseServiceClient.addStock(stockRequest);
                item.setQuantityReceived(item.getQuantityOrdered());
            } catch (Exception e) {
                log.error("Failed to update warehouse stock for productId={}, warehouseId={}: {}",
                        item.getProductId(), item.getWarehouseId(), e.getMessage(), e);
                throw new RuntimeException(
                        "Warehouse stock update failed for productId=" + item.getProductId()
                                + ". Receive goods aborted.");
            }
        }

        po.setStatus(PoStatus.RECEIVED);
        po.setReceivedDate(LocalDateTime.now());
        po.setReceivedBy(request.getPerformedBy());
        PurchaseOrder saved = poRepository.save(po);

        for (PurchaseOrderItem item : saved.getItems()) {
            try {
                MovementLogRequest movementRequest = new MovementLogRequest(
                        item.getWarehouseId(),
                        item.getProductId(),
                        "STOCK_IN",
                        item.getQuantityOrdered(),
                        saved.getId(),
                        "PURCHASE_ORDER",
                        "Received from PO #" + saved.getId(),
                        request.getPerformedBy()
                );
                movementServiceClient.createMovement(movementRequest);
            } catch (Exception e) {
                log.error("Movement logging failed for productId={}, poId={}. Stock was already updated. Error: {}",
                        item.getProductId(), saved.getId(), e.getMessage(), e);
            }
        }

        if (po.getExpectedDate() != null && po.getExpectedDate().isBefore(LocalDate.now())) {
            try {
                eventPublisher.publishPoOverdue(saved.getId(), saved.getSupplierId(), saved.getExpectedDate());
            } catch (Exception e) {
                log.error("Failed to publish PO_OVERDUE event: {}", e.getMessage(), e);
            }
        }

        return mapToResponse(saved);
    }

    private PurchaseOrder findPoOrThrow(Long id) {
        return poRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Purchase order not found with id: " + id));
    }

    private PurchaseOrderResponse mapToResponse(PurchaseOrder po) {
        List<PurchaseOrderItemResponse> itemResponses = po.getItems().stream()
                .map(item -> PurchaseOrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .warehouseId(item.getWarehouseId())
                        .quantityOrdered(item.getQuantityOrdered())
                        .quantityReceived(item.getQuantityReceived())
                        .unitPrice(item.getUnitPrice())
                        .createdAt(item.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .supplierId(po.getSupplierId())
                .status(po.getStatus())
                .orderedBy(po.getOrderedBy())
                .approvedBy(po.getApprovedBy())
                .receivedBy(po.getReceivedBy())
                .notes(po.getNotes())
                .expectedDate(po.getExpectedDate())
                .receivedDate(po.getReceivedDate())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .items(itemResponses)
                .build();
    }
}
