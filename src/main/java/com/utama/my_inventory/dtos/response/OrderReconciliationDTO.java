package com.utama.my_inventory.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReconciliationDTO {

    private Long purchaseOrderId;
    private String orderNumber;
    private String supplierName;

    @Builder.Default
    private List<MatchedItemDTO> matchedItems = new ArrayList<>();

    @Builder.Default
    private List<PartialItemDTO> partialItems = new ArrayList<>();

    @Builder.Default
    private List<MissingItemDTO> missingItems = new ArrayList<>();

    @Builder.Default
    private List<ExtraItemDTO> extraItems = new ArrayList<>();

    private ReconciliationSummary summary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatchedItemDTO {
        private String sku;
        private String productName;
        private Integer orderedQuantity;
        private Integer receivedQuantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PartialItemDTO {
        private String sku;
        private String productName;
        private Integer orderedQuantity;
        private Integer receivedQuantity;
        private Integer pendingQuantity;
        private BigDecimal unitPrice;
        private String observation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MissingItemDTO {
        private String sku;
        private String productName;
        private Integer orderedQuantity;
        private Integer receivedQuantity;
        private Integer missingQuantity;
        private String observation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExtraItemDTO {
        private String sku;
        private String productName;
        private Integer receivedQuantity;
        private String observation;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReconciliationSummary {
        private Integer totalOrderedItems;
        private Integer totalReceivedItems;
        private Integer totalMatchedQuantity;
        private Integer totalPartialQuantity;
        private Integer totalMissingQuantity;
        private Integer totalExtraQuantity;
        private BigDecimal totalOrderValue;
        private BigDecimal totalReceivedValue;
        private String recommendation;
        private boolean hasDiscrepancies;
    }
}