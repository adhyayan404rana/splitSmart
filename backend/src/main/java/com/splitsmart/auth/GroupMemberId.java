package com.splitsmart.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GroupMemberId implements Serializable {
    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "user_id")
    private UUID userId;
}
