package com.splitsmart.auth;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "group_members")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberEntity {

    @EmbeddedId
    private GroupMemberId id;

    @Column(nullable = false)
    private String role; // OWNER, MEMBER

    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt;

    @PrePersist
    protected void onCreate() {
        joinedAt = Instant.now();
    }
}
