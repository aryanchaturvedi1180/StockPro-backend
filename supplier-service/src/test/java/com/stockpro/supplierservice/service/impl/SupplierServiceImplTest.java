package com.stockpro.supplierservice.service.impl;

import com.stockpro.supplierservice.dto.SupplierRequest;
import com.stockpro.supplierservice.entity.Supplier;
import com.stockpro.supplierservice.exception.DuplicateResourceException;
import com.stockpro.supplierservice.exception.ResourceNotFoundException;
import com.stockpro.supplierservice.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierServiceImpl supplierService;

    private SupplierRequest request;
    private Supplier supplier;

    @BeforeEach
    void setUp() {
        request = new SupplierRequest();
        request.setName("Tech Vendor Pvt Ltd");
        request.setContactName("Ravi Kumar");
        request.setEmail("vendor@stockpro.com");
        request.setPhone("9876543210");
        request.setAddress("Noida");

        supplier = Supplier.builder()
                .id(1L)
                .name(request.getName())
                .contactName(request.getContactName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .isActive(true)
                .build();
    }

    @Test
    void createShouldSaveSupplierWhenEmailIsUnique() {
        when(supplierRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(supplier);

        var response = supplierService.create(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("vendor@stockpro.com");
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void createShouldFailWhenEmailAlreadyExists() {
        when(supplierRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> supplierService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void deactivateShouldMarkSupplierInactive() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = supplierService.deactivate(1L);

        assertThat(response.isActive()).isFalse();
        verify(supplierRepository).save(supplier);
    }

    @Test
    void deleteShouldFailWhenSupplierMissing() {
        when(supplierRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> supplierService.delete(7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Supplier not found");
    }
}
