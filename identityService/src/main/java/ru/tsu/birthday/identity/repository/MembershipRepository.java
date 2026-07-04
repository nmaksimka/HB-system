package ru.tsu.birthday.identity.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.identity.entity.GroupMembership;
import java.util.*;

public interface MembershipRepository extends JpaRepository<GroupMembership, UUID> {
    List<GroupMembership> findByUserId(UUID userId);
    List<GroupMembership> findByGroupId(UUID groupId);
    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
    void deleteByGroupIdAndUserId(UUID groupId, UUID userId);
}
