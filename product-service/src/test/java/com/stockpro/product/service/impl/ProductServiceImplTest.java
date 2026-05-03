package com.stockpro.product.service.impl;

import com.stockpro.product.dto.ProductRequest;
import com.stockpro.product.dto.ProductResponse;
import com.stockpro.product.entity.Product;
import com.stockpro.product.exception.DuplicateResourceException;
import com.stockpro.product.exception.ResourceNotFoundException;
import com.stockpro.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
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

    private ProductRequest request;
    private Product product;

    @BeforeEach
    void setUp() {
        request = new ProductRequest();
        request.setName("Dell Monitor");
        request.setSku("MON-001");
        request.setPrice(BigDecimal.valueOf(12000));
        request.setReorderLevel(5);
        request.setBarcode("890000000001");

        product = Product.builder()
                .id(1L)
                .name("Dell Monitor")
                .sku("MON-001")
                .price(BigDecimal.valueOf(12000))
                .reorderLevel(5)
                .barcode("890000000001")
                .isActive(true)
                .build();
    }

    @Test
    void createShouldSaveProductWhenSkuIsUnique() {
        when(productRepository.existsBySku("MON-001")).thenReturn(false);
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.create(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getSku()).isEqualTo("MON-001");
        assertThat(response.isActive()).isTrue();
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createShouldFailWhenSkuAlreadyExists() {
        when(productRepository.existsBySku("MON-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU already exists");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateShouldFailWhenNewSkuBelongsToAnotherProduct() {
        Product existing = Product.builder().id(1L).name("Old").sku("OLD-001").price(BigDecimal.TEN).reorderLevel(2).build();
        when(productRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(productRepository.existsBySku("MON-001")).thenReturn(true);

        assertThatThrownBy(() -> productService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU already exists");
    }

    @Test
    void getByIdShouldFailWhenProductMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void getAllActiveShouldReturnOnlyActiveProducts() {
        when(productRepository.findByIsActive(true)).thenReturn(List.of(product));

        List<ProductResponse> products = productService.getAllActive();

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Dell Monitor");
    }
}
