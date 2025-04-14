package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.errors.FriendRequestNotFoundException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.enums.InvitationStatus;
import ch.uzh.ifi.hase.soprafs24.repository.FriendRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import javax.persistence.EntityExistsException;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

public class FriendRequestServiceTest {


        @InjectMocks
        private FriendRequestService friendRequestService;

        @InjectMocks
        private UserService userService;

        @Mock
        private FriendRequestRepository friendRequestRepository;

        @BeforeEach
        void setup() {
            friendRequestRepository = Mockito.mock(FriendRequestRepository.class);
            userService = Mockito.mock(UserService.class);
            friendRequestService = new FriendRequestService(userService, friendRequestRepository);
        }


    @Test
    void getFriendRequestById_validId_Success() throws FriendRequestNotFoundException {
        Long friendRequestId = 1L;
        FriendRequest mockFriendRequest = new FriendRequest();
        mockFriendRequest.setId(friendRequestId);

        when(friendRequestRepository.findById(friendRequestId)).thenReturn(Optional.of(mockFriendRequest));

        FriendRequest result = friendRequestService.getFriendRequestById(friendRequestId);

        assertNotNull(result);
        assertEquals(friendRequestId, result.getId());
    }

    @Test
    void getFriendRequestById_invalidId_friendRequestNotFound() {
        Long friendRequestId = 1L;

        when(friendRequestRepository.findById(friendRequestId)).thenReturn(Optional.empty());

        assertThrows(FriendRequestNotFoundException.class, () -> friendRequestService.getFriendRequestById(friendRequestId));
    }

    @Test
    void getFriendRequestsByTargetId_validId_successful() {
        Long targetId = 1L;
        FriendRequest mockRequest = new FriendRequest();
        mockRequest.setTarget(new User());
        mockRequest.setSender(new User());

        when(friendRequestRepository.findByTargetId(targetId)).thenReturn(List.of(mockRequest));

        assertFalse(friendRequestService.getFriendRequestsByTargetId(targetId).isEmpty());
    }

    @Test
    void getFriendRequestsByTargetId_invalidId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> friendRequestService.getFriendRequestsByTargetId(null));
    }


    @Test
    void getFriendRequestsBySenderId_validId_returnsListSuccessfully() {
        Long senderId = 1L;
        FriendRequest mockRequest = new FriendRequest();
        mockRequest.setTarget(new User());
        mockRequest.setSender(new User());

        when(friendRequestRepository.findBySenderId(senderId)).thenReturn(List.of(mockRequest));

        assertFalse(friendRequestService.getFriendRequestsBySenderId(senderId).isEmpty());
    }

    // Test createFriendRequest
    @Test
    void createFriendRequest_validData_Success() throws UserNotFoundException, EntityExistsException {
        User sender = new User();
        sender.setId(1L);
        User target = new User();
        target.setId(2L);

        when(userService.getUserById(sender.getId())).thenReturn(Optional.of(sender));
        when(userService.getUserById(target.getId())).thenReturn(Optional.of(target));
        when(friendRequestRepository.findBySenderAndTarget(sender, target)).thenReturn(Optional.empty());
        when(friendRequestRepository.saveAndFlush(Mockito.any(FriendRequest.class))).thenAnswer(invocation -> {
            FriendRequest friendRequest = invocation.getArgument(0);
            friendRequest.setId(10L); // Simulate generated ID
            return friendRequest;
        });

        FriendRequest createdRequest = friendRequestService.createFriendRequest(sender, target, "Hello");

        assertNotNull(createdRequest.getId());
        assertEquals(InvitationStatus.PENDING, createdRequest.getStatus());
        assertEquals("Hello", createdRequest.getMessage());
        assertEquals(createdRequest.getSender(), sender);
        assertEquals(createdRequest.getTarget(), target);
        assertNotNull(createdRequest.getTimeStamp());
    }

    @Test
    void createFriendRequest_invalidSender_throwsUserNotFound() {
        User sender = new User();
        sender.setId(1L);
        User target = new User();
        target.setId(2L);

        when(userService.getUserById(sender.getId())).thenReturn(Optional.empty());
        when(userService.getUserById(target.getId())).thenReturn(Optional.of(target));

        assertThrows(UserNotFoundException.class, () -> friendRequestService.createFriendRequest(sender, target, "Hello"));
    }

    @Test
    void createFriendRequest_friendRequestAlreadyExists_throwsEntityExistsException() {
        User sender = new User();
        sender.setId(1L);
        User target = new User();
        target.setId(2L);

        when(userService.getUserById(sender.getId())).thenReturn(Optional.of(sender));
        when(userService.getUserById(target.getId())).thenReturn(Optional.of(target));
        when(friendRequestRepository.findBySenderAndTarget(sender, target)).thenReturn(Optional.of(new FriendRequest()));

        assertThrows(EntityExistsException.class, () -> friendRequestService.createFriendRequest(sender, target, "Hello"));
    }


    @Test
    void updateFriendRequest_validData_Successfully() throws IllegalArgumentException {
        Long friendRequestId = 1L;
        FriendRequest existingRequest = new FriendRequest();
        existingRequest.setId(friendRequestId);
        existingRequest.setStatus(InvitationStatus.PENDING);

        when(friendRequestRepository.findById(friendRequestId)).thenReturn(Optional.of(existingRequest));
        when(friendRequestRepository.saveAndFlush(Mockito.any(FriendRequest.class))).thenAnswer(invocation -> {
            FriendRequest friendRequest = invocation.getArgument(0);
            return friendRequest;
        });

        FriendRequest updatedRequest = friendRequestService.updateFriendRequest(existingRequest, InvitationStatus.ACCEPTED);

        assertNotNull(updatedRequest);
        assertEquals(InvitationStatus.ACCEPTED, updatedRequest.getStatus());
    }

    @Test
    void updateFriendRequest_InvalidStatus_throwsIllegalArgumentException() {
        Long friendRequestId = 1L;
        FriendRequest existingRequest = new FriendRequest();
        existingRequest.setId(friendRequestId);
        existingRequest.setStatus(InvitationStatus.ACCEPTED);

        when(friendRequestRepository.findById(friendRequestId)).thenReturn(Optional.of(existingRequest));

        assertThrows(IllegalArgumentException.class, () -> friendRequestService.updateFriendRequest(existingRequest, InvitationStatus.PENDING));
    }


}
