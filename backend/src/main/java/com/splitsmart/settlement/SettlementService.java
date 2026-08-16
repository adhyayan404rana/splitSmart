package com.splitsmart.settlement;

import org.springframework.stereotype.Service;

/**
 * Settlement & DSA Graph Math Engine Module Boundary
 * Responsible for Debt Simplification (Phase 1 Netting, Phase 2 DP-Bitmasking exact solver for N<15,
 * Phase 3 Greedy Two-Heap fallback for N>=15).
 */
@Service
public class SettlementService {
    public String getModuleInfo() {
        return "Settlement Engine (DSA Graph Math) Module Initialized";
    }
}
