package com.stockpro.supplierservice.service.impl;

import com.stockpro.supplierservice.dto.SupplierRequest;
import com.stockpro.supplierservice.entity.Supplier;
import com.stockpro.supplierservice.exception.DuplicateResourceException;
import com.stockpro.supplierservice.exception.ResourceNotFoundException;
import com.stockpro.supplierservice.repository.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
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

    @Test
    void createShouldSaveSupplierWhenEmailIsUnique() {
        SupplierRequest request = request("Tech Supplier", "tech@stockpro.com");
        Supplier saved = supplier(1L, "Tech Supplier", "tech@stockpro.com", true);

        when(supplierRepository.existsByEmail("tech@stockpro.com")).thenReturn(false);
        when(supplierRepository.save(any(Supplier.class))).thenReturn(saved);

        var response = supplierService.create(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("tech@stockpro.com");
        assertThat(response.isActive()).isTrue();
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void createShouldThrowWhenEmailAlreadyExists() {
        SupplierRequest request = request("Tech Supplier", "tech@stockpro.com");

        when(supplierRepository.existsByEmail("tech@stockpro.com")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void getByIdShouldReturnSupplier() {
        Supplier supplier = supplier(1L, "Tech Supplier", "tech@stockpro.com", true);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(supplier));

        var response = supplierService.getById(1L);

        assertThat(response.getName()).isEqualTo("Tech Supplier");
        assertThat(response.getEmail()).isEqualTo("tech@stockpro.com");
    }

    @Test
    void getByIdShouldThrowWhenMissing() {
        when(supplierRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplierService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Supplier not found");
    }

    @Test
    void getAllShouldReturnAllSuppliers() {
        when(supplierRepository.findAll()).thenReturn(List.of(
                supplier(1L, "Tech Supplier", "tech@stockpro.com", true),
                supplier(2L, "Office Supplier", "office@stockpro.com", true)
        ));

        var responses = supplierService.getAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getEmail()).isEqualTo("tech@stockpro.com");
        assertThat(responses.get(1).getEmail()).isEqualTo("office@stockpro.com");
    }

    @Test
    void getAllActiveShouldReturnOnlyActiveSuppliers() {
        when(supplierRepository.findByIsActive(true)).thenReturn(List.of(
                supplier(1L, "Tech Supplier", "tech@stockpro.com", true)
        ));

        var responses = supplierService.getAllActive();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isActive()).isTrue();
    }

    @Test
    void updateShouldUpdateSupplierWhenEmailIsSame() {
        Supplier existing = supplier(1L, "Old Supplier", "tech@stockpro.com", true);
        SupplierRequest request = request("Updated Supplier", "tech@stockpro.com");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(existing)).thenReturn(existing);

        var response = supplierService.update(1L, request);

        assertThat(response.getName()).isEqualTo("Updated Supplier");
        assertThat(response.getEmail()).isEqualTo("tech@stockpro.com");
        verify(supplierRepository, never()).existsByEmail("tech@stockpro.com");
    }

    @Test
    void updateShouldThrowWhenNewEmailAlreadyExists() {
        Supplier existing = supplier(1L, "Old Supplier", "old@stockpro.com", true);
        SupplierRequest request = request("Updated Supplier", "new@stockpro.com");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.existsByEmail("new@stockpro.com")).thenReturn(true);

        assertThatThrownBy(() -> supplierService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");

        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void activateShouldSetSupplierActive() {
        Supplier existing = supplier(1L, "Tech Supplier", "tech@stockpro.com", false);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(existing)).thenReturn(existing);

        var response = supplierService.activate(1L);

        assertThat(response.isActive()).isTrue();
        verify(supplierRepository).save(existing);
    }

    @Test
    void deactivateShouldSetSupplierInactive() {
        Supplier existing = supplier(1L, "Tech Supplier", "tech@stockpro.com", true);

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(supplierRepository.save(existing)).thenReturn(existing);

        var response = supplierService.deactivate(1L);

        assertThat(response.isActive()).isFalse();
        verify(supplierRepository).save(existing);
    }

    @Test
    void deleteShouldDeleteWhenSupplierExists() {
        when(supplierRepository.existsById(1L)).thenReturn(true);

        supplierService.delete(1L);

        verify(supplierRepository).deleteById(1L);
    }

    @Test
    void deleteShouldThrowWhenSupplierMissing() {
        when(supplierRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> supplierService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Supplier not found");

        verify(supplierRepository, never()).deleteById(anyLong());
    }

    private SupplierRequest request(String name, String email) {
        SupplierRequest request = new SupplierRequest();
        request.setName(name);
        request.setContactName("Contact Person");
        request.setEmail(email);
        request.setPhone("9876543210");
        request.setAddress("Noida, Uttar Pradesh");
        return request;
    }

    private Supplier supplier(Long id, String name, String email, boolean active) {
        return Supplier.builder()
                .id(id)
                .name(name)
                .contactName("Contact Person")
                .email(email)
                .phone("9876543210")
                .address("Noida, Uttar Pradesh")
                .isActive(active)
                .build();
    }
}