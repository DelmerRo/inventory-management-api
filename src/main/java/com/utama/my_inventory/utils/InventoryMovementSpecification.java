package com.utama.my_inventory.utils;

import com.utama.my_inventory.dtos.request.MovementFilter;
import com.utama.my_inventory.entities.InventoryMovement;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.List;

public class InventoryMovementSpecification {

    public static Specification<InventoryMovement> filter(MovementFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (f.productId() != null) {
                predicates.add(cb.equal(root.get("product").get("id"), f.productId()));
            }

            if (f.movementType() != null) {
                predicates.add(cb.equal(root.get("movementType"), f.movementType()));
            }

            if (StringUtils.hasText(f.registeredBy())) {
                predicates.add(cb.like(
                        cb.lower(root.get("registeredBy")),
                        "%" + f.registeredBy().trim().toLowerCase() + "%"
                ));
            }

            if (f.startDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("movementDate"), f.startDate()));
            }

            if (f.endDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("movementDate"), f.endDate()));
            }

            // El ordenamiento se puede definir aquí o pasar por el repositorio
            query.orderBy(cb.desc(root.get("movementDate")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}