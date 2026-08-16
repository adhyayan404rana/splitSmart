package com.splitsmart.api;

import com.splitsmart.auth.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                     @Valid @RequestBody CreateGroupRequest request) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(groupService.createGroup(userPrincipal.getId(), request));
    }

    @PostMapping("/join")
    public ResponseEntity<GroupResponse> joinGroup(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                   @Valid @RequestBody JoinGroupRequest request) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(groupService.joinGroup(userPrincipal.getId(), request));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> getUserGroups(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(groupService.getUserGroups(userPrincipal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroupDetails(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                         @PathVariable UUID id) {
        if (userPrincipal == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(groupService.getGroupDetails(id, userPrincipal.getId()));
    }
}
