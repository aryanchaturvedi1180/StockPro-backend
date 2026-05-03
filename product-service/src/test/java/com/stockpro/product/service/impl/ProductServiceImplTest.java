package com.stockpro.product.service.impl;

import com.stockpro.product.dto.ProductRequest;
import com.stockpro.product.entity.Product;
import com.stockpro.product.exception.DuplicateResourceException;
import com.stockpro.product.exception.ResourceNotFoundException;
import com.stockpro.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void createShouldSaveProductWhenSkuIsUnique() {
        ProductRequest request = request("Laptop", "SKU-001");

        Product saved = product(1L, "Laptop", "SKU-001", true);

        when(productRepository.existsBySku("SKU-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(saved);

        var response = productService.create(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSku()).isEqualTo("SKU-001");
        assertThat(response.isActive()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createShouldThrowWhenSkuAlreadyExists() {
        ProductRequest request = request("Laptop", "SKU-001");

        when(productRepository.existsBySku("SKU-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU already exists");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void getByIdShouldReturnProduct() {
        Product product = product(1L, "Laptop", "SKU-001", true);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        var response = productService.getById(1L);

        assertThat(response.getName()).isEqualTo("Laptop");
        assertThat(response.getSku()).isEqualTo("SKU-001");
    }

    @Test
    void getByIdShouldThrowWhenMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void getBySkuShouldReturnProduct() {
        Product product = product(1L, "Laptop", "SKU-001", true);

        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(product));

        var response = productService.getBySku("SKU-001");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Laptop");
    }

    @Test
    void getBySkuShouldThrowWhenMissing() {
        when(productRepository.findBySku("BAD-SKU")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getBySku("BAD-SKU"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with SKU");
    }

    @Test
    void getAllShouldReturnAllProducts() {
        when(productRepository.findAll()).thenReturn(List.of(
                product(1L, "Laptop", "SKU-001", true),
                product(2L, "Mouse", "SKU-002", true)
        ));

        var responses = productService.getAll();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getSku()).isEqualTo("SKU-001");
        assertThat(responses.get(1).getSku()).isEqualTo("SKU-002");
    }

    @Test
    void getAllActiveShouldReturnOnlyActiveProducts() {
        when(productRepository.findByIsActive(true)).thenReturn(List.of(
                product(1L, "Laptop", "SKU-001", true)
        ));

        var responses = productService.getAllActive();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).isActive()).isTrue();
    }

    @Test
    void searchByNameShouldReturnMatchingProducts() {
        when(productRepository.findByNameContainingIgnoreCase("lap")).thenReturn(List.of(
                product(1L, "Laptop", "SKU-001", true)
        ));

        var responses = productService.searchByName("lap");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    void updateShouldUpdateProductWhenSkuIsSame() {
        Product existing = product(1L, "Old Laptop", "SKU-001", true);
        ProductRequest request = request("New Laptop", "SKU-001");

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        var response = productService.update(1L, request);

        assertThat(response.getName()).isEqualTo("New Laptop");
        assertThat(response.getSku()).isEqualTo("SKU-001");
        verify(productRepository, never()).existsBySku("SKU-001");
    }

    @Test
    void updateShouldThrowWhenNewSkuAlreadyExists() {
        Product existing = product(1L, "Laptop", "SKU-001", true);
        ProductRequest request = request("Laptop", "SKU-999");

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySku("SKU-999")).thenReturn(true);

        assertThatThrownBy(() -> productService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU already exists");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void activateShouldSetProductActive() {
        Product existing = product(1L, "Laptop", "SKU-001", false);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        var response = productService.activate(1L);

        assertThat(response.isActive()).isTrue();
        verify(productRepository).save(existing);
    }

    @Test
    void deactivateShouldSetProductInactive() {
        Product existing = product(1L, "Laptop", "SKU-001", true);

        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);

        var response = productService.deactivate(1L);

        assertThat(response.isActive()).isFalse();
        verify(productRepository).save(existing);
    }

    @Test
    void deleteShouldDeleteWhenProductExists() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.delete(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteShouldThrowWhenProductMissing() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");

        verify(productRepository, never()).deleteById(anyLong());
    }

    private ProductRequest request(String name, String sku) {
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setSku(sku);
        request.setPrice(BigDecimal.valueOf(1000));
        request.setReorderLevel(10);
        request.setBarcode("BAR-" + sku);
        return request;
    }

    private Product product(Long id, String name, String sku, boolean active) {
        return Product.builder()
                .id(id)
                .name(name)
                .sku(sku)
                .price(BigDecimal.valueOf(1000))
                .reorderLevel(10)
                .barcode("BAR-" + sku)
                .isActive(active)
                .build();
    }
}