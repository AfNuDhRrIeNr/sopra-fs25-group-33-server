package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.errors.FriendRequestNotFoundException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.enums.InvitationStatus;
import ch.uzh.ifi.hase.soprafs24.repository.FriendRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.persistence.EntityExistsException;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class FriendRequestService {

    private final Logger log = LoggerFactory.getLogger(FriendRequestService.class);

    private final UserService userService;

    private final FriendRequestRepository friendRequestRepository;
    @Autowired
    public FriendRequestService(@Qualifier("userService") UserService userService, @Qualifier("friendRequestRepository") FriendRequestRepository friendRequestRepository) {
        this.userService = userService;
        this.friendRequestRepository = friendRequestRepository;
    }

    public FriendRequest getFriendRequestById(Long id) throws FriendRequestNotFoundException {
        if(id == null) throw new IllegalArgumentException("Id cannot be null");
        Optional<FriendRequest> optional = friendRequestRepository.findById(id);
        if (optional.isEmpty()) throw new FriendRequestNotFoundException("Friend request with id "+id.toString()+" not found");
        return optional.get();
    }

    public List<FriendRequest> getFriendRequestsByTargetId(Long targetId) {
        if(targetId == null) throw new IllegalArgumentException("TargetId cannot be null");
        return friendRequestRepository.findByTargetId(targetId);
    }

    public List<FriendRequest> getFriendRequestsBySenderId(Long senderId) {
        if(senderId == null) throw new IllegalArgumentException("SenderId cannot be null");
        return friendRequestRepository.findBySenderId(senderId);
    }

    public FriendRequest createFriendRequest(User sender, User target, String message) throws UserNotFoundException, EntityExistsException, IllegalArgumentException {

        if (sender == null || target == null) throw new IllegalArgumentException("Sender and target cannot be null");
        if (sender.getId() == target.getId()) throw new IllegalArgumentException("Cannot send friend request to yourself");

        if (userService.getUserById(sender.getId()).isEmpty()) throw new UserNotFoundException("Sender not found");
        if (userService.getUserById(target.getId()).isEmpty()) throw new UserNotFoundException("Target not found");

        if(friendRequestRepository.findBySenderAndTarget(sender, target).isPresent()) throw new EntityExistsException("Friend request already exists");

        if (message == null) message = "";

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setTarget(target);
        friendRequest.setSender(sender);
        friendRequest.setStatus(InvitationStatus.PENDING);
        friendRequest.setMessage(message);
        friendRequest.setTimeStamp(LocalDateTime.now());

        return friendRequestRepository.saveAndFlush(friendRequest);
    }

    public FriendRequest updateFriendRequest(FriendRequest friendRequest, InvitationStatus status) throws IllegalArgumentException {
        if(friendRequest == null || friendRequestRepository.findById(friendRequest.getId()).isEmpty())
            throw new IllegalArgumentException("Friend request cannot be null");

        if(status == null) throw new IllegalArgumentException("Status cannot be null");

        if(friendRequest.getStatus() != InvitationStatus.PENDING && status == InvitationStatus.PENDING)
            throw new IllegalArgumentException("Cannot change status from"+friendRequest.getStatus().toString()+"to PENDING");

        friendRequest.setStatus(status);
        return friendRequestRepository.saveAndFlush(friendRequest);
    }
}
