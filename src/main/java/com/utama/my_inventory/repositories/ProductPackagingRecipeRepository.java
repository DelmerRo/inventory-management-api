// com/utama/my_inventory/repositories/ProductPackagingRecipeRepository.java
package com.utama.my_inventory.repositories;

import com.utama.my_inventory.entities.ProductPackagingRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductPackagingRecipeRepository extends JpaRepository<ProductPackagingRecipe, Long> {

    // Este query usa JOIN FETCH para traer el Insumo (Supply) de una vez
    // y evitar problemas de Lazy Loading al leer el CPP (Costo Unitario).
    @Query("SELECT r FROM ProductPackagingRecipe r JOIN FETCH r.supply WHERE r.product.id = :productId")
    List<ProductPackagingRecipe> findByProductIdWithSupplies(@Param("productId") Long productId);
}