package com.splitsmart.ledger;

import org.springframework.stereotype.Service;

/**
 * Core Ledger & Event Sourcing Module Boundary
 * Responsible for Append-Only Event Store, Command Processing, Optimistic Concurrency Control (OCC),
 * and Materialized View Projections for group balances.
 */
@Service
public class LedgerService {
    public String getModuleInfo() {
        return "Core Event-Sourced Ledger Module Initialized";
    }
}
