package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("friendRequestRepository")
public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    Optional<FriendRequest> findById(Long id);

    List<FriendRequest> findByTargetId(Long targetId);

    List<FriendRequest> findBySenderId(Long senderId);

    Optional<FriendRequest> findBySenderAndTarget(User sender, User target);
}
