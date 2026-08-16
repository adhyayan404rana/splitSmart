package com.splitsmart.ledger;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifyDraftRequest {

    @NotNull(message = "OCC Version integer tag is required")
    private Integer expectedVersion;

    @Min(value = 1, message = "Total amount must be greater than 0")
    private Long totalAmountCents;

    private String description;
    private String category;
    private List<String> participants;
}
