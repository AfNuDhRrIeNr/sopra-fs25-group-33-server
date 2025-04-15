package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

  private final Logger log = LoggerFactory.getLogger(UserService.class);

  private final UserRepository userRepository;

  @Autowired
  public UserService(@Qualifier("userRepository") UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> getUsers() {
    return this.userRepository.findAll();
  }

  public User createUser(User newUser) {

    //first checks if username already taken
    checkIfUserExists(newUser);
    newUser.setToken(UUID.randomUUID().toString());
    newUser.setStatus(UserStatus.ONLINE);
    // saves the given entity but data is only persisted in the database once
    // flush() is called
    newUser = userRepository.save(newUser);
    userRepository.flush();

    log.debug("Created Information for User: {}", newUser);
    return newUser;
  }

  /**
   * This is a helper method that will check the uniqueness criteria of the
   * username and the name
   * defined in the User entity. The method will do nothing if the input is unique
   * and throw an error otherwise.
   *
   * @param userToBeCreated
   * @throws org.springframework.web.server.ResponseStatusException
   * @see User
   */

  public void checkIfUserExists(User userToBeCreated) {

    //retrieves the username if it already exists
    User existingUser = userRepository.findByUsername(userToBeCreated.getUsername());

    //generates an error message if this is the case
    if (existingUser != null) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists!");
    }
  }

  public Optional<User> getUserByToken(String token) {
    return userRepository.findByToken(token);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public User getUserByUsername(String username) { return userRepository.findByUsername(username); }
  public User authenticateUser(User userInput) {
    // Find user by username
    User user = userRepository.findByUsername(userInput.getUsername());

    // If user is not found or password is incorrect, return null
    if (user == null || !user.getPassword().equals(userInput.getPassword())) {
        return null;
    }

    // Set status to ONLINE upon successful login
    user.setStatus(UserStatus.ONLINE);
    userRepository.save(user);  
    userRepository.flush();  

    // Return authenticated user with token
    return user;
  }

    public User updateUserStatus(User user, UserStatus userStatus) {
      user.setStatus(userStatus);
      return userRepository.saveAndFlush(user);
    }

    public void checkAndAddFriend(User sender, User target) {
      if(sender == null || target == null) throw new IllegalArgumentException("Sender and target cannot be null");
      if(sender == target) throw new IllegalArgumentException("Cannot add yourself as a friend");
      if(!sender.getFriends().contains(target)) {
          sender.addFriend(target);
          userRepository.saveAndFlush(sender);
      }
      if(!target.getFriends().contains(sender)) {
          target.addFriend(sender);
          userRepository.saveAndFlush(target);
      }
    }
}
