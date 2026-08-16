package com.splitsmart.settlement;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarkSettledRequest {
    @NotNull
    private UUID groupId;

    @NotNull
    private UUID debtorId;

    @NotNull
    private UUID creditorId;

    @Min(1)
    private long amountCents;

    private String currency;
    private String note;
    private String transactionRef;
}
