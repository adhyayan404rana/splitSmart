package com.splitsmart.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final AuthService authService;

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Transactional
    public GroupResponse createGroup(UUID userId, CreateGroupRequest request) {
        String inviteCode = generateUniqueInviteCode();

        GroupEntity group = GroupEntity.builder()
                .name(request.getName())
                .inviteCode(inviteCode)
                .createdBy(userId)
                .build();

        group = groupRepository.save(group);

        // Add creator as OWNER
        GroupMemberId memberId = new GroupMemberId(group.getId(), userId);
        GroupMemberEntity member = GroupMemberEntity.builder()
                .id(memberId)
                .role("OWNER")
                .build();
        groupMemberRepository.save(member);

        return mapToGroupResponse(group, userId);
    }

    @Transactional
    public GroupResponse joinGroup(UUID userId, JoinGroupRequest request) {
        GroupEntity group = groupRepository.findByInviteCode(request.getInviteCode().trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite code"));

        if (!groupMemberRepository.existsByIdGroupIdAndIdUserId(group.getId(), userId)) {
            GroupMemberId memberId = new GroupMemberId(group.getId(), userId);
            GroupMemberEntity member = GroupMemberEntity.builder()
                    .id(memberId)
                    .role("MEMBER")
                    .build();
            groupMemberRepository.save(member);
        }

        return mapToGroupResponse(group, userId);
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> getUserGroups(UUID userId) {
        List<GroupMemberEntity> memberships = groupMemberRepository.findByIdUserId(userId);
        return memberships.stream()
                .map(m -> groupRepository.findById(m.getId().getGroupId()).orElse(null))
                .filter(g -> g != null)
                .map(g -> mapToGroupResponse(g, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GroupResponse getGroupDetails(UUID groupId, UUID userId) {
        if (!groupMemberRepository.existsByIdGroupIdAndIdUserId(groupId, userId)) {
            throw new IllegalArgumentException("Access denied: You are not a member of this group");
        }

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found"));

        return mapToGroupResponse(group, userId);
    }

    public GroupResponse mapToGroupResponse(GroupEntity group, UUID callerUserId) {
        List<GroupMemberEntity> members = groupMemberRepository.findByIdGroupId(group.getId());
        
        String callerRole = members.stream()
                .filter(m -> m.getId().getUserId().equals(callerUserId))
                .map(GroupMemberEntity::getRole)
                .findFirst()
                .orElse("MEMBER");

        List<UserResponse> memberUsers = members.stream()
                .map(m -> userRepository.findById(m.getId().getUserId()).orElse(null))
                .filter(u -> u != null)
                .map(authService::mapToUserResponse)
                .collect(Collectors.toList());

        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .inviteCode(group.getInviteCode())
                .createdBy(group.getCreatedBy())
                .userRole(callerRole)
                .members(memberUsers)
                .createdAt(group.getCreatedAt())
                .build();
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
            }
            code = sb.toString();
        } while (groupRepository.existsByInviteCode(code));
        return code;
    }
}
