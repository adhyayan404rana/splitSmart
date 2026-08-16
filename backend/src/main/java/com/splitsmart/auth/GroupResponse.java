package com.splitsmart.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {
    private UUID id;
    private String name;
    private String inviteCode;
    private UUID createdBy;
    private String userRole; // OWNER or MEMBER for current caller
    private List<UserResponse> members;
    private Instant createdAt;
}
