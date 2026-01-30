package com.utama.my_inventory.utils;

import com.utama.my_inventory.entities.InventoryMovement;
import com.utama.my_inventory.entities.enums.MovementType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class InventoryMovementSpecification {

    public static Specification<InventoryMovement> filter(
            Long productId,
            MovementType movementType,
            String registeredBy,
            LocalDateTime startDate,
            LocalDateTime endDate
    ) {

        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }

            if (movementType != null) {
                predicates.add(cb.equal(root.get("movementType"), movementType));
            }

            if (registeredBy != null && !registeredBy.isBlank()) {
                predicates.add(
                        cb.like(
                                cb.lower(root.get("registeredBy")),
                                "%" + registeredBy.toLowerCase() + "%"
                        )
                );
            }

            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("movementDate"), startDate));
            }

            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("movementDate"), endDate));
            }

            query.orderBy(cb.desc(root.get("movementDate")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
