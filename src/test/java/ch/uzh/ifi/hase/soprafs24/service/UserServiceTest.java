package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserService userService;

  private User testUser;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.openMocks(this);

    // given
    testUser = new User();
    testUser.setId(1L);
    testUser.setUsername("testUsername");
    testUser.setPassword("testPassword");

    // when -> any object is being saved in the userRepository -> return the dummy testUser
    Mockito.when(userRepository.save(Mockito.any())).thenReturn(testUser);
  }

  @Test
  public void createUser_validInputs_success() {
    // when -> any object is being saved in the userRepository -> return the dummy testUser
    User createdUser = userService.createUser(testUser);

    // then
    Mockito.verify(userRepository, Mockito.times(1)).save(Mockito.any());

    assertEquals(testUser.getId(), createdUser.getId());
    assertEquals(testUser.getUsername(), createdUser.getUsername());
    assertNotNull(createdUser.getToken());
    assertEquals(UserStatus.ONLINE, createdUser.getStatus());
  }

  @Test
  public void createUser_duplicateName_throwsException() {
    // given -> a first user has already been created
    userService.createUser(testUser);

    // when -> setup additional mocks for UserRepository
    Mockito.when(userRepository.findByUsername(Mockito.any())).thenReturn(testUser);

    // then -> attempt to create second user with same user -> check that an error is thrown
    assertThrows(ResponseStatusException.class, () -> userService.createUser(testUser));
  }

  @Test
  public void authenticateUser_validCredentials_success() {
    // given -> a user exists in the repository
    Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(testUser);

    // when
    User authenticatedUser = userService.authenticateUser(testUser);

    // then
    assertNotNull(authenticatedUser);
    assertEquals(testUser.getUsername(), authenticatedUser.getUsername());
    assertEquals(UserStatus.ONLINE, authenticatedUser.getStatus());
  }

  @Test
  public void authenticateUser_invalidPassword_returnsNull() {
    // given -> a user exists in the repository
    Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(testUser);

    // when -> attempt to authenticate with the wrong password
    User invalidUser = new User();
    invalidUser.setUsername("testUsername");
    invalidUser.setPassword("wrongPassword");

    User authenticatedUser = userService.authenticateUser(invalidUser);

    // then
    assertNull(authenticatedUser);
  }

  @Test
  public void authenticateUser_nonExistentUsername_returnsNull() {
    // given -> no user exists in the repository
    Mockito.when(userRepository.findByUsername("nonExistentUser")).thenReturn(null);

    // when -> attempt to authenticate with a non-existent username
    User nonExistentUser = new User();
    nonExistentUser.setUsername("nonExistentUser");
    nonExistentUser.setPassword("anyPassword");

    User authenticatedUser = userService.authenticateUser(nonExistentUser);

    // then
    assertNull(authenticatedUser);
  }

  @Test
  public void updateUserStatus_setsStatusAndSaves() {
      Mockito.when(userRepository.saveAndFlush(testUser)).thenReturn(testUser);

      User updated = userService.updateUserStatus(testUser, UserStatus.IN_GAME);

      assertEquals(UserStatus.IN_GAME, updated.getStatus());
      Mockito.verify(userRepository).saveAndFlush(testUser);
  }

  @Test
  public void checkAndAddFriend_addsFriendIfNotPresent() {
      User friend = new User();
      friend.setId(2L);
      friend.setUsername("friend");
      testUser.setFriends(new java.util.ArrayList<>());
      friend.setFriends(new java.util.ArrayList<>());

      Mockito.when(userRepository.saveAndFlush(testUser)).thenReturn(testUser);
      Mockito.when(userRepository.saveAndFlush(friend)).thenReturn(friend);

      userService.checkAndAddFriend(testUser, friend);

      assertTrue(testUser.getFriends().contains(friend));
      assertTrue(friend.getFriends().contains(testUser));
  }

  @Test
  public void checkAndAddFriend_nullSender_throwsException() {
      User friend = new User();
      friend.setId(2L);
      assertThrows(IllegalArgumentException.class, () -> userService.checkAndAddFriend(null, friend));
  }

  @Test
  public void checkAndAddFriend_nullTarget_throwsException() {
      assertThrows(IllegalArgumentException.class, () -> userService.checkAndAddFriend(testUser, null));
  }

  @Test
  public void checkAndAddFriend_addYourself_throwsException() {
      assertThrows(IllegalArgumentException.class, () -> userService.checkAndAddFriend(testUser, testUser));
  }

  @Test
  public void getUserById_returnsUser() {
      Mockito.when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));
      assertTrue(userService.getUserById(1L).isPresent());
  }

  @Test
  public void getUserByToken_returnsUser() {
      Mockito.when(userRepository.findByToken("token")).thenReturn(java.util.Optional.of(testUser));
      assertTrue(userService.getUserByToken("token").isPresent());
  }

  @Test
  public void getUserByUsername_returnsUser() {
      Mockito.when(userRepository.findByUsername("testUsername")).thenReturn(testUser);
      User found = userService.getUserByUsername("testUsername");
      assertEquals(testUser, found);
  }

  @Test
  public void getUsers_returnsAllUsers() {
      java.util.List<User> users = java.util.List.of(testUser);
      Mockito.when(userRepository.findAll()).thenReturn(users);
      assertEquals(users, userService.getUsers());
  }

  @Test
  public void createUser_emptyUsername_throwsException() {
      User user = new User();
      user.setUsername("");
      user.setPassword("pw");
      assertThrows(ResponseStatusException.class, () -> userService.createUser(user));
  }

  @Test
  public void createUser_nullUsername_throwsException() {
      User user = new User();
      user.setUsername(null);
      user.setPassword("pw");
      assertThrows(ResponseStatusException.class, () -> userService.createUser(user));
  }

  @Test
  public void createUser_emptyPassword_throwsException() {
      User user = new User();
      user.setUsername("user");
      user.setPassword("");
      assertThrows(ResponseStatusException.class, () -> userService.createUser(user));
  }

  @Test
  public void createUser_nullPassword_throwsException() {
      User user = new User();
      user.setUsername("user");
      user.setPassword(null);
      assertThrows(ResponseStatusException.class, () -> userService.createUser(user));
  }
}
