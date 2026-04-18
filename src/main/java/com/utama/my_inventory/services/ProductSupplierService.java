package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.response.SupplierAssociationResponseDTO;
import java.util.List;

public interface ProductSupplierService {
    List<SupplierAssociationResponseDTO> getProductsBySupplier(Long supplierId);
    List<SupplierAssociationResponseDTO> getAllRelations();
    SupplierAssociationResponseDTO getRelation(Long supplierId, Long productId);
    SupplierAssociationResponseDTO getRelationById(Long id);
    Long countProductsBySupplier(Long supplierId);
    SupplierAssociationResponseDTO updateSku(Long id, String sku);
    SupplierAssociationResponseDTO togglePrimary(Long id);
    void deleteRelation(Long id);
}