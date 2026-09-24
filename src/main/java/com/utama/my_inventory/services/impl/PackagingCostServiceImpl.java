// com/utama/my_inventory/services/impl/PackagingCostServiceImpl.java
package com.utama.my_inventory.services.impl;

import com.utama.my_inventory.dtos.response.product.PackagingBreakdownDTO;
import com.utama.my_inventory.entities.Product;
import com.utama.my_inventory.entities.ProductPackagingRecipe;
import com.utama.my_inventory.entities.Supply;
import com.utama.my_inventory.exceptions.BusinessException;
import com.utama.my_inventory.repositories.ProductPackagingRecipeRepository;
import com.utama.my_inventory.services.PackagingCostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PackagingCostServiceImpl implements PackagingCostService {

    private final ProductPackagingRecipeRepository recipeRepository;

    @Override
    public PackagingCostResult calculatePackagingCost(Product product) {
        if (product == null || product.getId() == null) {
            log.error("Intento de cálculo de empaque con un producto nulo o sin ID.");
            throw new BusinessException("No se puede calcular el empaque de un producto inválido.");
        }

        log.debug("Iniciando cálculo de empaque para el producto ID: {}", product.getId());
        List<ProductPackagingRecipe> recipes = recipeRepository.findByProductIdWithSupplies(product.getId());

        BigDecimal totalPackagingCost = BigDecimal.ZERO;
        List<PackagingBreakdownDTO> breakdown = new ArrayList<>();

        if (recipes.isEmpty()) {
            log.debug("El producto ID: {} no tiene receta de empaque configurada.", product.getId());
            return new PackagingCostResult(totalPackagingCost, breakdown);
        }

        for (ProductPackagingRecipe recipe : recipes) {
            try {
                Supply supply = recipe.getSupply();

                if (!supply.getActive()) {
                    log.warn("El insumo ID: {} ({}) está inactivo. Se excluye del cálculo.", supply.getId(), supply.getName());
                    continue; // Se omite si el insumo fue dado de baja temporalmente
                }

                BigDecimal quantityNeeded = calculateQuantity(product, recipe);
                BigDecimal currentCpp = getValidUnitCost(supply);

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
            } catch (Exception e) {
                log.error("Error al calcular el insumo ID: {} para el producto ID: {}. Motivo: {}",
                        recipe.getSupply().getId(), product.getId(), e.getMessage());
                throw new BusinessException("Error calculando el costo de empaque: " + e.getMessage());
            }
        }

        log.debug("Cálculo de empaque finalizado para producto ID: {}. Costo Total: {}", product.getId(), totalPackagingCost);
        return new PackagingCostResult(totalPackagingCost, breakdown);
    }

    @Override
    public Map<Long, PackagingCostResult> calculatePackagingCostBatch(List<Product> products) {
        if (products == null || products.isEmpty()) return new HashMap<>();

        // 1. Extraemos los IDs
        List<Long> productIds = products.stream().map(Product::getId).toList();

        // 2. Hacemos 1 sola consulta a la BD
        List<ProductPackagingRecipe> allRecipes = recipeRepository.findByProductIdInWithSupplies(productIds);

        // 3. Agrupamos las recetas por ID de producto
        Map<Long, List<ProductPackagingRecipe>> recipesByProduct = allRecipes.stream()
                .collect(Collectors.groupingBy(r -> r.getProduct().getId()));

        Map<Long, PackagingCostResult> packagingMap = new HashMap<>();

        // 4. Calculamos el empaque de cada producto en RAM
        for (Product product : products) {
            List<ProductPackagingRecipe> recipes = recipesByProduct.getOrDefault(product.getId(), new ArrayList<>());

            BigDecimal totalPackagingCost = BigDecimal.ZERO;
            List<PackagingBreakdownDTO> breakdown = new ArrayList<>();

            for (ProductPackagingRecipe recipe : recipes) {
                Supply supply = recipe.getSupply();
                if (!supply.getActive()) continue;

                BigDecimal quantityNeeded = calculateQuantity(product, recipe); // Tu método privado actual
                BigDecimal currentCpp = getValidUnitCost(supply);               // Tu método privado actual
                BigDecimal itemTotalCost = quantityNeeded.multiply(currentCpp).setScale(2, RoundingMode.HALF_UP);

                totalPackagingCost = totalPackagingCost.add(itemTotalCost);
                breakdown.add(new PackagingBreakdownDTO(
                        supply.getId(), supply.getName(), supply.getUnitMeasure(),
                        quantityNeeded.setScale(4, RoundingMode.HALF_UP), currentCpp, itemTotalCost
                ));
            }

            packagingMap.put(product.getId(), new PackagingCostResult(totalPackagingCost, breakdown));
        }

        return packagingMap;
    }

    private BigDecimal calculateQuantity(Product product, ProductPackagingRecipe recipe) {
        BigDecimal multiplier = recipe.getMultiplier();

        return switch (recipe.getCalculationType()) {
            case FIJO -> multiplier;
            case AREA_CM2 -> {
                validateDimensions(product);
                // Área total = 2 * (L*W + L*H + W*H)
                BigDecimal lw = product.getLength().multiply(product.getWidth());
                BigDecimal lh = product.getLength().multiply(product.getHeight());
                BigDecimal wh = product.getWidth().multiply(product.getHeight());
                BigDecimal area = lw.add(lh).add(wh).multiply(BigDecimal.valueOf(2));
                yield area.multiply(multiplier);
            }
            case PERIMETRO_CM -> {
                validateDimensions(product);
                // Perímetro volumétrico base = 2 * (L + W + H)
                BigDecimal perimeter = product.getLength()
                        .add(product.getWidth())
                        .add(product.getHeight())
                        .multiply(BigDecimal.valueOf(2));
                yield perimeter.multiply(multiplier);
            }
        };
    }

    private void validateDimensions(Product product) {
        if (product.getLength() == null || product.getWidth() == null || product.getHeight() == null ||
                product.getLength().compareTo(BigDecimal.ZERO) <= 0 ||
                product.getWidth().compareTo(BigDecimal.ZERO) <= 0 ||
                product.getHeight().compareTo(BigDecimal.ZERO) <= 0) {

            throw new BusinessException(String.format(
                    "El producto '%s' requiere dimensiones de largo, ancho y alto mayores a 0 para el cálculo volumétrico.",
                    product.getName()
            ));
        }
    }

    private BigDecimal getValidUnitCost(Supply supply) {
        if (supply.getUnitCost() == null || supply.getUnitCost().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El insumo '" + supply.getName() + "' tiene un costo unitario inválido o no configurado.");
        }
        return supply.getUnitCost();
    }
}