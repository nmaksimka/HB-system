package ru.tsu.birthday.identity.application;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.tsu.birthday.identity.domain.GroupMembership;
import java.util.*;

public interface MembershipRepository extends JpaRepository<GroupMembership, UUID> {
    List<GroupMembership> findByUserId(UUID userId);
    List<GroupMembership> findByGroupId(UUID groupId);
    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);
}
