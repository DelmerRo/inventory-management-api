package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.request.SupplierRequestDTO;
import com.utama.my_inventory.dtos.response.SupplierResponseDTO;
import com.utama.my_inventory.dtos.response.SupplierSummaryResponseDTO;
import com.utama.my_inventory.entities.Supplier;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.exceptions.ResourceNotFoundException;
import com.utama.my_inventory.mapper.SupplierMapper;
import com.utama.my_inventory.repositories.SupplierRepository;
import com.utama.my_inventory.services.SupplierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SupplierServiceImpl implements SupplierService {

    private final SupplierRepository supplierRepository;
    private final SupplierMapper supplierMapper;

    @Override
    @Transactional
    @CacheEvict(value = {"suppliers", "supplierSummary"}, allEntries = true)
    public SupplierResponseDTO createSupplier(SupplierRequestDTO requestDTO) {
        log.info("Creating new supplier: {}", requestDTO.name());

        validateUniqueSupplierName(requestDTO.name());

        Supplier supplier = supplierMapper.toEntity(requestDTO);
        Supplier savedSupplier = supplierRepository.save(supplier);

        log.info("Supplier created with ID: {}", savedSupplier.getId());
        return supplierMapper.toResponseDTO(savedSupplier);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "suppliers", key = "#id")
    public SupplierResponseDTO getSupplierById(Long id) {
        log.info("Retrieving supplier with ID: {}", id);
        Supplier supplier = findActiveSupplierById(id);
        return supplierMapper.toResponseDTO(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    public SupplierResponseDTO getSupplierByName(String name) {
        log.info("Retrieving supplier with name: {}", name);
        Supplier supplier = supplierRepository.findByNameAndActiveTrue(name)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with name: " + name));
        return supplierMapper.toResponseDTO(supplier);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "suppliers", key = "'all'")
    public List<SupplierResponseDTO> getAllSuppliers() {
        log.info("Retrieving all active suppliers");
        List<Supplier> suppliers = supplierRepository.findByActiveTrueOrderByNameAsc();
        return supplierMapper.toResponseDTOList(suppliers);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "supplierSummary", key = "'all'")
    public List<SupplierSummaryResponseDTO> getAllSuppliersSummary() {
        log.info("Retrieving all suppliers summary");
        List<Supplier> suppliers = supplierRepository.findByActiveTrueOrderByNameAsc();
        return supplierMapper.toSummaryDTOList(suppliers);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"suppliers", "supplierSummary"}, key = "#id")
    public SupplierResponseDTO updateSupplier(Long id, SupplierRequestDTO requestDTO) {
        log.info("Updating supplier with ID: {}", id);

        Supplier supplier = findActiveSupplierById(id);

        // Validar nombre único si cambió
        if (!supplier.getName().equals(requestDTO.name())) {
            validateUniqueSupplierName(requestDTO.name());
        }

        supplierMapper.updateEntityFromDTO(requestDTO, supplier);
        Supplier updatedSupplier = supplierRepository.save(supplier);

        log.info("Supplier updated with ID: {}", id);
        return supplierMapper.toResponseDTO(updatedSupplier);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"suppliers", "supplierSummary"}, key = "#id")
    public void deleteSupplier(Long id) {
        log.info("Soft deleting supplier with ID: {}", id);

        Supplier supplier = findActiveSupplierById(id);

        // Validar que no tenga productos activos
        if (supplier.countActiveProducts() > 0) {
            throw new BusinessException(
                    "Cannot delete supplier '%s' because it has %d associated active products"
                            .formatted(supplier.getName(), supplier.countActiveProducts())
            );
        }

        supplier.setActive(false);
        supplierRepository.save(supplier);

        log.info("Supplier soft deleted with ID: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"suppliers", "supplierSummary"}, key = "#id")
    public SupplierResponseDTO toggleSupplierStatus(Long id) {
        log.info("Toggling status for supplier with ID: {}", id);

        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found"));

        supplier.setActive(!supplier.getActive());
        Supplier updatedSupplier = supplierRepository.save(supplier);

        String status = updatedSupplier.getActive() ? "activated" : "deactivated";
        log.info("Supplier {} with ID: {}", status, id);
        return supplierMapper.toResponseDTO(updatedSupplier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponseDTO> searchSuppliers(String name, String contactPerson, String email) {
        log.info("Searching suppliers with filters - name: {}, contactPerson: {}, email: {}",
                name, contactPerson, email);

        List<Supplier> suppliers = supplierRepository.searchSuppliers(name, contactPerson, email);
        return supplierMapper.toResponseDTOList(suppliers);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierResponseDTO> getSuppliersWithActiveProducts() {
        log.info("Retrieving suppliers with active products");

        List<Supplier> suppliers = supplierRepository.findByActiveTrue();

        return suppliers.stream()
                .filter(supplier -> supplier.countActiveProducts() > 0)
                .map(supplierMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getTotalSupplierCount() {
        return supplierRepository.countActiveSuppliers();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierSummaryResponseDTO> getTopSuppliersByProductCount(int limit) {
        log.info("Retrieving top {} suppliers by product count", limit);

        List<Supplier> suppliers = supplierRepository.findByActiveTrue();

        return suppliers.stream()
                .sorted(Comparator.comparingLong((Supplier s) -> s.countActiveProducts()).reversed())
                .limit(limit)
                .map(supplierMapper::toSummaryDTO)
                .collect(Collectors.toList());
    }

    private Supplier findActiveSupplierById(Long id) {
        return supplierRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Supplier not found or inactive with ID: " + id
                ));
    }

    private void validateUniqueSupplierName(String name) {
        if (supplierRepository.existsByNameAndActiveTrue(name)) {
            throw new BusinessException("Supplier already exists with name: " + name);
        }
    }
}