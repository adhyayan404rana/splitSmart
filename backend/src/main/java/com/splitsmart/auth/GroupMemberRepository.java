package com.splitsmart.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMemberEntity, GroupMemberId> {
    List<GroupMemberEntity> findByIdUserId(UUID userId);
    List<GroupMemberEntity> findByIdGroupId(UUID groupId);
    boolean existsByIdGroupIdAndIdUserId(UUID groupId, UUID userId);
}
