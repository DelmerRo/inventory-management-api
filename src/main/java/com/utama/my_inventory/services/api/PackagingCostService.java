// com/utama/my_inventory/services/api/PackagingCostService.java
package com.utama.my_inventory.services.api;

import com.utama.my_inventory.dtos.response.product.PackagingBreakdownDTO;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.ProductPackagingRecipe;
import com.utama.my_inventory.entities.Supply;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.repositories.ProductPackagingRecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PackagingCostService {

    private final ProductPackagingRecipeRepository recipeRepository;

    @Transactional(readOnly = true)
    public PackagingCostResult calculatePackagingCost(Product product) {
        if (product == null || product.getId() == null) {
            throw new BusinessException("No se puede calcular el empaque de un producto inválido.");
        }

        // Usamos el nuevo método del repositorio
        List<ProductPackagingRecipe> recipes = recipeRepository.findByProductIdWithSupplies(product.getId());

        BigDecimal totalPackagingCost = BigDecimal.ZERO;
        List<PackagingBreakdownDTO> breakdown = new ArrayList<>();

        if (recipes.isEmpty()) {
            return new PackagingCostResult(totalPackagingCost, breakdown);
        }

        for (ProductPackagingRecipe recipe : recipes) {
            Supply supply = recipe.getSupply();

            if (!supply.getActive()) {
                log.warn("El insumo ID: {} ({}) está inactivo. Se excluye del cálculo.", supply.getId(), supply.getName());
                continue;
            }

            BigDecimal quantityNeeded = calculateQuantity(product, recipe);
            BigDecimal currentCpp = supply.getUnitCost();

            if (currentCpp == null || currentCpp.compareTo(BigDecimal.ZERO) < 0) {
                log.error("Costo inválido en insumo ID: {}", supply.getId());
                currentCpp = BigDecimal.ZERO;
            }

            BigDecimal itemTotalCost = quantityNeeded.multiply(currentCpp).setScale(2, RoundingMode.HALF_UP);
            totalPackagingCost = totalPackagingCost.add(itemTotalCost);

            breakdown.add(new PackagingBreakdownDTO(
                    supply.getId(),
                    supply.getName(),
                    supply.getUnitMeasure(),
                    quantityNeeded.setScale(4, RoundingMode.HALF_UP),
                    currentCpp,
                    itemTotalCost
            ));
        }

        return new PackagingCostResult(totalPackagingCost, breakdown);
    }

    private BigDecimal calculateQuantity(Product product, ProductPackagingRecipe recipe) {
        BigDecimal multiplier = recipe.getMultiplier();
        BigDecimal length = product.getLength() != null ? product.getLength() : BigDecimal.ZERO;
        BigDecimal width = product.getWidth() != null ? product.getWidth() : BigDecimal.ZERO;
        BigDecimal height = product.getHeight() != null ? product.getHeight() : BigDecimal.ZERO;

        return switch (recipe.getCalculationType()) {
            case FIJO -> multiplier;
            case AREA_CM2 -> {
                BigDecimal lw = length.multiply(width);
                BigDecimal lh = length.multiply(height);
                BigDecimal wh = width.multiply(height);
                BigDecimal area = lw.add(lh).add(wh).multiply(BigDecimal.valueOf(2));
                yield area.multiply(multiplier);
            }
            case PERIMETRO_CM -> {
                BigDecimal perimeter = length.add(width).add(height).multiply(BigDecimal.valueOf(2));
                yield perimeter.multiply(multiplier);
            }
        };
    }

    public record PackagingCostResult(BigDecimal totalCost, List<PackagingBreakdownDTO> breakdown) {}
}