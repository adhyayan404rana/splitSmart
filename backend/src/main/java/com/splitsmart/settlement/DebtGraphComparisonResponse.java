package com.splitsmart.settlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtGraphComparisonResponse {
    private int rawTransactionCount;
    private int simplifiedTransactionCount;
    private double reductionPercentage; // e.g. 66.67%
    private List<RawDebtResponse> rawTransactions;
    private List<SimplifiedDebtResponse> simplifiedTransactions;
}
