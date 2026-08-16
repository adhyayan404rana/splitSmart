package com.splitsmart.ledger;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "expense_drafts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DraftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "payer_id", nullable = false)
    private UUID payerId;

    @Column(name = "payer_name")
    private String payerName;

    @Column(name = "total_amount_cents", nullable = false)
    private long totalAmountCents;

    @Column(nullable = false)
    private String currency;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category;

    @Column(name = "split_logic", columnDefinition = "TEXT")
    private String splitLogic;

    @Column(name = "participants", columnDefinition = "TEXT")
    private String participants; // Comma separated or JSON string

    @Column(nullable = false)
    private String status; // DRAFT, COMMITTED, REJECTED

    @Column(name = "is_disputed", nullable = false)
    private boolean isDisputed;

    @Column(name = "dispute_reason", columnDefinition = "TEXT")
    private String disputeReason;

    @Column(nullable = false)
    private int version; // Optimistic Concurrency Control (OCC) version tag

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
