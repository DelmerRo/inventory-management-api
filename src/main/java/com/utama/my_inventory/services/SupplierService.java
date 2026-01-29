package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.SupplierRequestDTO;
import com.utama.my_inventory.dtos.response.SupplierResponseDTO;
import com.utama.my_inventory.dtos.response.SupplierSummaryResponseDTO;

import java.util.List;

public interface SupplierService {

    SupplierResponseDTO createSupplier(SupplierRequestDTO requestDTO);
    SupplierResponseDTO getSupplierById(Long id);
    SupplierResponseDTO getSupplierByName(String name);
    List<SupplierResponseDTO> getAllSuppliers();
    List<SupplierSummaryResponseDTO> getAllSuppliersSummary();
    SupplierResponseDTO updateSupplier(Long id, SupplierRequestDTO requestDTO);
    void deleteSupplier(Long id);
    SupplierResponseDTO toggleSupplierStatus(Long id);

    List<SupplierResponseDTO> searchSuppliers(String name, String contactPerson, String email);
    List<SupplierResponseDTO> getSuppliersWithActiveProducts();

    Long getTotalSupplierCount();
    List<SupplierSummaryResponseDTO> getTopSuppliersByProductCount(int limit);
}