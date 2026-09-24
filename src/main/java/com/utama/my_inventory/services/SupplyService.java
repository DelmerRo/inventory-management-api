package com.utama.my_inventory.services;

import com.utama.my_inventory.dtos.request.supply.SupplyMovementRequestDTO;
import com.utama.my_inventory.dtos.request.supply.SupplyRequestDTO;
import com.utama.my_inventory.dtos.request.supply.SupplyResponseDTO;
import com.utama.my_inventory.dtos.response.supply.SupplyMovementResponseDTO;

import java.util.List;

public interface SupplyService {
    List<SupplyResponseDTO> getAllActiveSupplies();
    SupplyResponseDTO getSupplyById(Long id);
    SupplyResponseDTO createSupply(SupplyRequestDTO dto);
    SupplyResponseDTO updateSupply(Long id, SupplyRequestDTO dto);
    void deactivateSupply(Long id);
    SupplyMovementResponseDTO registerMovement(Long supplyId, SupplyMovementRequestDTO dto);
    List<SupplyMovementResponseDTO> getMovementsBySupply(Long supplyId);
}