package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.response.SupplierAssociationResponseDTO;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.ProductSupplier;
import com.utama.my_inventory.entities.Supplier;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.exceptions.ResourceNotFoundException;
import com.utama.my_inventory.repositories.ProductRepository;
import com.utama.my_inventory.repositories.ProductSupplierRepository;
import com.utama.my_inventory.repositories.SupplierRepository;
import com.utama.my_inventory.services.ProductSupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSupplierServiceImpl implements ProductSupplierService {

    private final ProductSupplierRepository productSupplierRepository;
    private final ProductRepository productRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SupplierAssociationResponseDTO> getProductsBySupplier(Long supplierId) {
        log.info("Getting products for supplier ID: {}", supplierId);

        // Verificar que el proveedor existe
        supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + supplierId));

        List<ProductSupplier> productSuppliers = productSupplierRepository.findBySupplierId(supplierId);

        return productSuppliers.stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "productSuppliers", key = "'all'")
    public List<SupplierAssociationResponseDTO> getAllRelations() {
        log.info("Getting all product-supplier relations");

        List<ProductSupplier> relations = productSupplierRepository.findAll();

        return relations.stream()
                .map(this::mapToResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierAssociationResponseDTO getRelation(Long supplierId, Long productId) {
        log.info("Getting relation between supplier {} and product {}", supplierId, productId);

        ProductSupplier relation = productSupplierRepository
                .findByProductIdAndSupplierId(productId, supplierId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Relation not found between supplier " + supplierId + " and product " + productId));

        return mapToResponseDTO(relation);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "productSuppliers", key = "#id")
    public SupplierAssociationResponseDTO getRelationById(Long id) {
        log.info("Getting product-supplier relation by ID: {}", id);

        ProductSupplier relation = productSupplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product-supplier relation not found with ID: " + id));

        return mapToResponseDTO(relation);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countProductsBySupplier(Long supplierId) {
        log.info("Counting products for supplier ID: {}", supplierId);

        // Verificar que el proveedor existe
        supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with ID: " + supplierId));

        return (long) productSupplierRepository.findBySupplierId(supplierId).size();
    }

    @Override
    @Transactional
    @CacheEvict(value = {"productSuppliers", "products", "productSummary"}, allEntries = true)
    public SupplierAssociationResponseDTO updateSku(Long id, String supplierSku) {
        log.info("Updating SKU for relation ID: {} to: {}", id, supplierSku);

        ProductSupplier relation = productSupplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product-supplier relation not found with ID: " + id));

        relation.setSupplierSku(supplierSku);
        ProductSupplier updatedRelation = productSupplierRepository.save(relation);

        log.info("SKU updated successfully for relation ID: {}", id);
        return mapToResponseDTO(updatedRelation);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"productSuppliers", "products", "productSummary"}, allEntries = true)
    public SupplierAssociationResponseDTO togglePrimary(Long id) {
        log.info("Toggling primary status for relation ID: {}", id);

        ProductSupplier relation = productSupplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product-supplier relation not found with ID: " + id));

        Long productId = relation.getProduct().getId();
        Boolean newPrimaryStatus = !relation.getIsPrimary();

        // Si vamos a marcar como principal, desmarcar otros
        if (newPrimaryStatus) {
            productSupplierRepository.findByProductIdAndIsPrimaryTrue(productId)
                    .ifPresent(primary -> {
                        primary.setIsPrimary(false);
                        productSupplierRepository.save(primary);
                    });
        }

        relation.setIsPrimary(newPrimaryStatus);
        ProductSupplier updatedRelation = productSupplierRepository.save(relation);

        log.info("Primary status toggled to: {} for relation ID: {}", newPrimaryStatus, id);
        return mapToResponseDTO(updatedRelation);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"productSuppliers", "products", "productSummary"}, allEntries = true)
    public void deleteRelation(Long id) {
        log.info("Deleting product-supplier relation with ID: {}", id);

        ProductSupplier relation = productSupplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product-supplier relation not found with ID: " + id));

        Long productId = relation.getProduct().getId();
        Long supplierId = relation.getSupplier().getId();
        boolean wasPrimary = relation.getIsPrimary();

        // Validar que no sea el único proveedor del producto
        long supplierCount = productSupplierRepository.findByProductId(productId).size();
        if (supplierCount <= 1) {
            throw new BusinessException(
                    "Cannot delete the only supplier of product. Product must have at least one supplier");
        }

        productSupplierRepository.delete(relation);

        // Si eliminamos el proveedor principal, marcar otro como principal
        if (wasPrimary) {
            productSupplierRepository.findByProductId(productId)
                    .stream()
                    .findFirst()
                    .ifPresent(newPrimary -> {
                        newPrimary.setIsPrimary(true);
                        productSupplierRepository.save(newPrimary);
                        log.info("New primary supplier set for product {}: {}", productId, newPrimary.getSupplier().getId());
                    });
        }

        log.info("Relation deleted successfully between product {} and supplier {}", productId, supplierId);
    }

    // ========== MÉTODOS PRIVADOS ==========

    private SupplierAssociationResponseDTO mapToResponseDTO(ProductSupplier ps) {
        return SupplierAssociationResponseDTO.builder()
                .id(ps.getId())
                .supplierId(ps.getSupplier().getId())
                .supplierName(ps.getSupplier().getName())
                .supplierSku(ps.getSupplierSku())
                .isPrimary(ps.getIsPrimary())
                .notes(ps.getNotes())
                .build();
    }
}