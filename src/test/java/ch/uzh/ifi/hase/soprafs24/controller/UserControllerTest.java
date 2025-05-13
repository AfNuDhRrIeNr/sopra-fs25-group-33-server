package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.constant.errors.FriendRequestNotFoundException;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.enums.InvitationStatus;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendRequestPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.FriendRequestPutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.FriendRequestService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.EntityExistsException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * This is a WebMvcTest which allows to test the UserController i.e. GET/POST
 * request without actually sending them over the network.
 * This tests if the UserController works.
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserService userService;

  @MockBean
  private FriendRequestService friendRequestService;

  @Test
  public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
    // given
    User user = new User();
    user.setUsername("firstname@lastname");
    user.setStatus(UserStatus.OFFLINE);

    List<User> allUsers = Collections.singletonList(user);

    // this mocks the UserService -> we define above what the userService should
    // return when getUsers() is called
    given(userService.getUsers()).willReturn(allUsers);

    // when
    MockHttpServletRequestBuilder getRequest = get("/users").contentType(MediaType.APPLICATION_JSON);

    // then
    mockMvc.perform(getRequest).andExpect(status().isOk())
        .andExpect(jsonPath("$", hasSize(1)))
        .andExpect(jsonPath("$[0].username", is(user.getUsername())))
        .andExpect(jsonPath("$[0].status", is(user.getStatus().toString())));
  }

  /**
   * Helper Method to convert userPostDTO into a JSON string such that the input
   * can be processed
   * Input will look like this: {"name": "Test User", "username": "testUsername"}
   * 
   * @param object
   * @return string
   */
  private String asJsonString(final Object object) {
    try {
      return new ObjectMapper().writeValueAsString(object);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          String.format("The request body could not be created.%s", e.toString()));
    }
  }

  @Test
  public void registerUser_validInput_userCreated201() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setUsername("testUsername");
    user.setPassword("testPassword");
    user.setToken("1");
    user.setStatus(UserStatus.ONLINE);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setUsername("testUsername");
    userPostDTO.setPassword("testPassword");

    given(userService.createUser(Mockito.any())).willReturn(user);

    // when/then -> do the request + validate the result
    MockHttpServletRequestBuilder postRequest = post("/users/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO)); // Ensures both username & password are sent

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

  @Test
  // POST /users/register Status: 409 CONFLICT
  public void registerUser_duplicateUsername_returnsConflict409() throws Exception {
    // given
    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setUsername("testUsername");
    userPostDTO.setPassword("testPassword");

    Mockito.doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists!"))
        .when(userService).createUser(Mockito.any());

    // when
    MockHttpServletRequestBuilder postRequest = post("/users/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isConflict()); // 409 Conflict
  }

  @Test
  // POST /users/login Status: 200 OK
  public void loginUser_validCredentials_returnsUser200() throws Exception {
    // given
    User user = new User();
    user.setId(1L);
    user.setUsername("testUsername");
    user.setPassword("testPassword");
    user.setToken("1");
    user.setStatus(UserStatus.ONLINE);

    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setUsername("testUsername");
    userPostDTO.setPassword("testPassword");

    given(userService.authenticateUser(Mockito.any())).willReturn(user);

    // when
    MockHttpServletRequestBuilder postRequest = post("/users/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is(user.getId().intValue())))
        .andExpect(jsonPath("$.username", is(user.getUsername())))
        .andExpect(jsonPath("$.status", is(user.getStatus().toString())));
  }

  @Test
  // POST /users/login Status: 401 UNAUTHORIZED
  public void loginUser_invalidCredentials_returnsUnauthorized401() throws Exception {
    // given
    UserPostDTO userPostDTO = new UserPostDTO();
    userPostDTO.setUsername("testUsername");
    userPostDTO.setPassword("wrongPassword");

    // Return null to simulate authentication failure
    given(userService.authenticateUser(Mockito.any())).willReturn(null);

    // when
    MockHttpServletRequestBuilder postRequest = post("/users/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(asJsonString(userPostDTO));

    // then
    mockMvc.perform(postRequest)
        .andExpect(status().isUnauthorized()); // 401 Unauthorized
  }

    @Test
    public void testGetAllUsers_WithUserIdFilter() throws Exception {
        // Assign
        User user = new User();
        user.setId(1L);
        user.setUsername("testUser");
        user.setHighScore(50);

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("anotherUser");
        user2.setHighScore(100);

        when(userService.getUsers()).thenReturn(List.of(user, user2));

        // Act & Assert
        mockMvc.perform(get("/users")
                        .param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username", is("testUser")));
    }

    @Test
    public void testGetAllUsers_WithLeaderboardSorting() throws Exception {
        // Assign
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("Alice");
        user1.setHighScore(100);

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("Bob");
        user2.setHighScore(200);

        when(userService.getUsers()).thenReturn(Arrays.asList(user1, user2));

        // Act & Assert
        mockMvc.perform(get("/users")
                        .param("leaderboard", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].username", is("Bob")))  // sorted descending
                .andExpect(jsonPath("$[1].username", is("Alice")));
    }

    @Test
    public void testGetAllUsers_WithoutParams_ReturnsAll() throws Exception {
        // Assign
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("Alice");

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("Bob");

        when(userService.getUsers()).thenReturn(List.of(user1, user2));

        // Act & Assert
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

  // --------------------------------------------- FriendRequests ---------------------------------------------

    // ----------------------------- GET FriendRequests -----------------------------
    @Test
    void getFriendRequests_validData_success() throws Exception {
        // Assign
        User mockUser = new User();
        Long userId = 1L;
        String token = "validTestToken";
        mockUser.setId(userId);
        mockUser.setToken(token);

        FriendRequest request = new FriendRequest();
        request.setId(2L);
        request.setTarget(mockUser);

        when(userService.getUserById(userId)).thenReturn(Optional.of(mockUser));
        when(userService.getUserByToken(token)).thenReturn(Optional.of(mockUser));
        when(friendRequestService.getFriendRequestsByTargetId(userId)).thenReturn(List.of(request));

        // Act & Assert
        MockHttpServletRequestBuilder getRequest = get("/users/friendRequests")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(request.getId().intValue())));
    }

    @Test
    void getFriendRequests_missingToken_unauthorized() throws Exception {
        Long userId = 1L;

        MockHttpServletRequestBuilder getRequest = get("/users/friendRequests")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "");

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Token is missing"));
    }

    // ----------------------------- POST FriendRequests -----------------------------
    @Test
    void createFriendRequest_validInput_returnsCreated() throws Exception {
        // Arrange
        User sender = new User();
        sender.setId(1L);
        sender.setToken("validToken");

        User target = new User();
        target.setId(2L);
        target.setUsername("fasdfasd");

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setId(10L);
        friendRequest.setSender(sender);
        friendRequest.setTarget(target);
        friendRequest.setMessage("Hi there!");

        FriendRequestPostDTO requestDTO = new FriendRequestPostDTO();
        requestDTO.setTargetUsername(target.getUsername());
        requestDTO.setMessage("Hi there!");

        given(userService.getUserByToken("validToken")).willReturn(Optional.of(sender));
        given(userService.getUserByUsername(target.getUsername())).willReturn(target);
        given(friendRequestService.createFriendRequest(sender, target, "Hi there!")).willReturn(friendRequest);

        MockHttpServletRequestBuilder postRequest = post("/users/friendRequests")
                .header("Authorization", "validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestDTO));

        // Act & Assert
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(friendRequest.getId().intValue())))
                .andExpect(jsonPath("$.sender.id", is(sender.getId().intValue())))
                .andExpect(jsonPath("$.target.id", is(target.getId().intValue())))
                .andExpect(jsonPath("$.message", is("Hi there!")))
                .andExpect(jsonPath("$.target.username", is(target.getUsername())));
    }

    @Test
    void createFriendRequest_missingToken_returnsUnauthorized() throws Exception {
        FriendRequestPostDTO requestDTO = new FriendRequestPostDTO();
        requestDTO.setTargetUsername("testUser");
        requestDTO.setMessage("Hello!");

        given(friendRequestService.createFriendRequest(Mockito.any(), Mockito.any(), Mockito.any()))
                .willThrow(new AssertionError("CreateFriendRequest was called even when it should not have been called!"));

        MockHttpServletRequestBuilder postRequest = post("/users/friendRequests")
                .header("Authorization", "")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Token is missing"));
    }

    @Test
    void createFriendRequest_missingTargetId_returnsBadRequest() throws Exception {
        User sender = new User();
        sender.setId(1L);
        sender.setToken("validToken");

        FriendRequestPostDTO requestDTO = new FriendRequestPostDTO();
        requestDTO.setMessage("Hi!");

        given(userService.getUserByToken("validToken")).willReturn(Optional.of(sender));
        given(friendRequestService.createFriendRequest(Mockito.any(), Mockito.any(), Mockito.any()))
                .willThrow(new AssertionError("CreateFriendRequest was called even when it should not have been called!"));

        MockHttpServletRequestBuilder postRequest = post("/users/friendRequests")
                .header("Authorization", "validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest())
                .andExpect(status().reason("Target username is missing"));
    }

    @Test
    void createFriendRequest_targetUserNotFound_returnsNotFound() throws Exception {
        User sender = new User();
        sender.setId(1L);
        sender.setToken("validToken");

        FriendRequestPostDTO requestDTO = new FriendRequestPostDTO();
        requestDTO.setTargetUsername("nonExistentUser");
        requestDTO.setMessage("Hello");

        given(userService.getUserByToken("validToken")).willReturn(Optional.of(sender));
        given(userService.getUserByUsername("nonExistentUser")).willReturn(null);
        given(friendRequestService.createFriendRequest(Mockito.any(), Mockito.any(), Mockito.any()))
                .willThrow(new AssertionError("CreateFriendRequest was called even when it should not have been called!"));

        MockHttpServletRequestBuilder postRequest = post("/users/friendRequests")
                .header("Authorization", "validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Target user not found"));
    }

    @Test
    public void createFriendRequest_targetUserAlreadyFriends_returnsConflict() throws Exception {
        // Assign
        User sender = new User();
        sender.setId(1L);
        sender.setToken("validToken");

        User target = new User();
        target.setId(2L);
        target.setUsername("testUser");

        FriendRequestPostDTO requestDTO = new FriendRequestPostDTO();
        requestDTO.setTargetUsername("testUser");
        requestDTO.setMessage("Hello!");

        given(userService.getUserByToken("validToken")).willReturn(Optional.of(sender));
        given(userService.getUserByUsername(target.getUsername())).willReturn(target);
        given(friendRequestService.createFriendRequest(sender, target, "Hello!"))
                .willThrow(new EntityExistsException("Friend request already exists"));

        MockHttpServletRequestBuilder postRequest = post("/users/friendRequests")
                .header("Authorization", "validToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestDTO));

        // Act & Assert
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict())
                .andExpect(status().reason("Friend request already exists"));
    }

    @Test
    public void createFriendRequest_senderNotFoundByToken_returnsUnauthorized() throws Exception {
        // Assign
        User target = new User();
        target.setId(2L);
        target.setUsername("afsdadsfa");

        FriendRequestPostDTO requestDTO = new FriendRequestPostDTO();
        requestDTO.setTargetUsername(target.getUsername());
        requestDTO.setMessage("Hello!");

        given(userService.getUserByToken("invalidToken")).willReturn(Optional.empty());
        given(friendRequestService.createFriendRequest(Mockito.any(), Mockito.any(), Mockito.any()))
                .willThrow(new AssertionError("CreateFriendRequest was called even when it should not have been called!"));

        MockHttpServletRequestBuilder postRequest = post("/users/friendRequests")
                .header("Authorization", "invalidToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(requestDTO));

        // Act & Assert
        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Invalid token: User not found"));
  }


    // ---------------------------- PUT FriendRequests ----------------------------

    @Test
    void updateFriendRequest_validInput_returnsOk() throws Exception {
        // Assign
        Long requestId = 10L;
        String token = "validToken";

        User user = new User();
        user.setId(1L);
        user.setToken(token);

        FriendRequest friendRequest = new FriendRequest();
        friendRequest.setId(requestId);
        friendRequest.setSender(user);  // the user is the sender
        friendRequest.setStatus(InvitationStatus.ACCEPTED);

        FriendRequestPutDTO updateDTO = new FriendRequestPutDTO();
        updateDTO.setStatus(InvitationStatus.ACCEPTED);

        given(friendRequestService.getFriendRequestById(requestId)).willReturn(friendRequest);
        given(userService.getUserByToken(token)).willReturn(Optional.of(user));
        given(friendRequestService.updateFriendRequest(friendRequest, InvitationStatus.ACCEPTED)).willReturn(friendRequest);

        MockHttpServletRequestBuilder putRequest = put("/users/friendRequests/{friendRequestId}", requestId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDTO));
        // Act & Assert
        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(friendRequest.getId().intValue())));
    }

    @Test
    void updateFriendRequest_invalidToken_returnsUnauthorized() throws Exception {
        // Assign
        FriendRequestPutDTO updateDTO = new FriendRequestPutDTO();
        updateDTO.setStatus(InvitationStatus.DECLINED);

        given(userService.getUserByToken("invalidToken")).willReturn(Optional.empty());
        given(friendRequestService.updateFriendRequest(Mockito.any(), Mockito.any()))
                .willThrow(new AssertionError("Update friend request was called but it should not have been called!"));

        MockHttpServletRequestBuilder putRequest = put("/users/friendRequests/1")
                .header("Authorization", "invalidToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDTO));

        // Act & Assert
        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("Invalid token: User not found"));
    }

    @Test
    void updateFriendRequest_userNotParticipant_returnsUnauthorized() throws Exception {
        // Assgin
        User attacker = new User(); attacker.setId(99L);
        User realSender = new User(); realSender.setId(1L);
        User realTarget = new User(); realTarget.setId(2L);

        FriendRequest request = new FriendRequest();
        request.setId(1L);
        request.setSender(realSender);
        request.setTarget(realTarget);

        FriendRequestPutDTO updateDTO = new FriendRequestPutDTO();
        updateDTO.setStatus(InvitationStatus.ACCEPTED);

        given(friendRequestService.getFriendRequestById(1L)).willReturn(request);
        given(userService.getUserByToken("attackerToken")).willReturn(Optional.of(attacker));
        given(friendRequestService.updateFriendRequest(Mockito.any(), Mockito.any()))
                .willThrow(new AssertionError("Update friend request was called but it should not have been called!"));

        MockHttpServletRequestBuilder putRequest = put("/users/friendRequests/1")
                .header("Authorization", "attackerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDTO));

        // Act & Assert
        mockMvc.perform(putRequest)
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason("User is not authorized to update this friend request"));
    }

    @Test
    void updateFriendRequest_notFoundFriendRequest_returnsNotFound() throws Exception {
        // Assign
        given(friendRequestService.getFriendRequestById(42L))
                .willThrow(new FriendRequestNotFoundException("Request not found"));
        given(friendRequestService.updateFriendRequest(Mockito.any(), Mockito.any()))
                .willThrow(new AssertionError("Update friend request was called but it should not have been called!"));

        FriendRequestPutDTO updateDTO = new FriendRequestPutDTO();
        updateDTO.setStatus(InvitationStatus.PENDING);

        MockHttpServletRequestBuilder putRequest = put("/users/friendRequests/42")
                .header("Authorization", "anyToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(updateDTO));

        // Act & Assert
        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound())
                .andExpect(status().reason("Request not found"));
    }

    @Test
    public void logoutUser_validToken_setsUserOffline() throws Exception {
        // given
        User user = new User();
        user.setId(1L);
        user.setUsername("testUsername");
        user.setStatus(UserStatus.ONLINE);
        user.setToken("valid-token");
        
        User offlineUser = new User();
        offlineUser.setId(1L);
        offlineUser.setUsername("testUsername");
        offlineUser.setStatus(UserStatus.OFFLINE);
        offlineUser.setToken("valid-token");
        
        given(userService.getUserByToken("valid-token")).willReturn(Optional.of(user));
        given(userService.updateUserStatus(user, UserStatus.OFFLINE)).willReturn(offlineUser);
        
        // when
        MockHttpServletRequestBuilder putRequest = put("/users/logout")
                .header("Authorization", "valid-token")
                .contentType(MediaType.APPLICATION_JSON);
        
        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(user.getId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.status", is(UserStatus.OFFLINE.toString())));
    }
}