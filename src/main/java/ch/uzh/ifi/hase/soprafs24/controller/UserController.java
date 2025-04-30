package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.constant.errors.FriendRequestNotFoundException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.FriendRequestService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.EntityExistsException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * User Controller
 * This class is responsible for handling all REST request that are related to
 * the user.
 * The controller will receive the request and delegate the execution to the
 * UserService and finally return the result.
 */
@RestController
public class UserController {

  private final UserService userService;

  private final FriendRequestService friendRequestService;

  UserController(UserService userService, FriendRequestService friendRequestService) {
      this.userService = userService;
      this.friendRequestService = friendRequestService;
  }

  @GetMapping("/users")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public List<UserGetDTO> getAllUsers(@RequestParam(required = false) Long userId, @RequestParam(value="leaderboard", required = false) boolean orderByBestGame) {
    // fetch all users in the internal representation
    List<User> users = userService.getUsers();
    List<UserGetDTO> userGetDTOs = new ArrayList<>();

    // convert each user to the API representation
    for (User user : users) {
      if(userId != null && user.getId() != userId) continue;
      userGetDTOs.add(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));
    }
    if(orderByBestGame) userGetDTOs.sort(Comparator.comparingInt(UserGetDTO::getHighScore).reversed());
    return userGetDTOs;
  }

  @PostMapping("/users/register")
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  public UserGetDTO createUser(@RequestBody UserPostDTO userPostDTO) {
    // convert API user to internal representation
    User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

    // create user
    User createdUser = userService.createUser(userInput);
    // convert internal representation of user back to API
    return DTOMapper.INSTANCE.convertEntityToUserGetDTO(createdUser);
  }


// POST /login - Authenticate a user and return their data if credentials are valid
  @PostMapping("/users/login")
  @ResponseStatus(HttpStatus.OK)
  @ResponseBody
  public UserGetDTO loginUser(@RequestBody UserPostDTO userPostDTO) {
      // Authenticate user
      User userInput = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

      User authenticatedUser = userService.authenticateUser(userInput);

      // If authentication fails, throw error (makes use of UserService's method authenticateUser)
      if (authenticatedUser == null) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
      }

      // Return authenticated user
      return DTOMapper.INSTANCE.convertEntityToUserGetDTO(authenticatedUser);
  }

    @PutMapping("/users/logout")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public UserGetDTO logoutUser(@RequestHeader("Authorization") String token) {
        // Verify token is provided
        if(token == null || token.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is missing");
        }
        
        // Find the user by token
        Optional<User> optionalUser = userService.getUserByToken(token);
        if(optionalUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token: User not found");
        }
        
        User user = optionalUser.get();
        
        // Update user status to OFFLINE
        user = userService.updateUserStatus(user, UserStatus.OFFLINE);
        
        // Return the updated user
        return DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);
    }

  // ------------------------------------------ FRIEND REQUESTS -------------------------------------------

    @GetMapping("/users/friendRequests")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<FriendRequestGetDTO> getFriendRequests(@RequestHeader("Authorization") String token) {
        // Verify user & token
        if(token == null || token.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is missing");
        Optional<User> optionalUser = userService.getUserByToken(token);
        if (optionalUser.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token: User not authorized to access this resource");

        User target = optionalUser.get();
        // fetch all friend requests in the internal representation
        List<FriendRequest> friendRequests = friendRequestService.getFriendRequestsByTargetId(target.getId());
        List<FriendRequestGetDTO> friendRequestGetDTOs = new ArrayList<>();

        // convert each friend request to the API representation
        for (FriendRequest friendRequest : friendRequests) {
            friendRequestGetDTOs.add(DTOMapper.INSTANCE.convertEntityToFriendRequestGetDTO(friendRequest));
        }
        return friendRequestGetDTOs;
    }


    @PostMapping("/users/friendRequests")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public FriendRequestGetDTO createFriendRequest(@RequestHeader("Authorization") String token, @RequestBody FriendRequestPostDTO friendRequestPostDTO) {
        // Verify user & token
        if (token == null || token.isEmpty())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is missing");
        Optional<User> optionalSender = userService.getUserByToken(token);
        if (optionalSender.isEmpty())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token: User not found");
        if (friendRequestPostDTO.getTargetUsername() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Target username is missing");
        User target = userService.getUserByUsername(friendRequestPostDTO.getTargetUsername());
        if (target == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found");

        // Add default message if left empty
        if (friendRequestPostDTO.getMessage() == null) friendRequestPostDTO.setMessage("");

        // Create friend request
        FriendRequest friendRequest;
        try {
            friendRequest = friendRequestService.createFriendRequest(optionalSender.get(), target, friendRequestPostDTO.getMessage());
        }
        catch (UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        catch (EntityExistsException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred:\n" + e.getMessage());
        }

        // Convert to DTO and return
        return DTOMapper.INSTANCE.convertEntityToFriendRequestGetDTO(friendRequest);
    }


    @PutMapping("/users/friendRequests/{friendRequestId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public FriendRequestGetDTO updateFriendRequest(@PathVariable Long friendRequestId, @RequestHeader("Authorization") String token, @RequestBody FriendRequestPutDTO friendRequestPutDTO) {
        // Verify user & token
        if(friendRequestId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Friend request ID cannot be null");
        if(token == null || token.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token is missing");

        // Fetch friend request
        FriendRequest friendRequest;
        try {
            friendRequest = friendRequestService.getFriendRequestById(friendRequestId);
        }
        catch (FriendRequestNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred:\n"+e.getMessage());
        }

        // Fetch user sending the put request
        Optional<User> optionalUser = userService.getUserByToken(token);
        if(optionalUser.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token: User not found");
        User user = optionalUser.get();

        // Check if user is part of the friend request
        if(!friendRequest.getSender().getId().equals(user.getId()) && !friendRequest.getTarget().getId().equals(user.getId()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authorized to update this friend request");
        FriendRequest updatedFriendRequest;
        try {
            // Update friend request
            updatedFriendRequest = friendRequestService.updateFriendRequest(friendRequest, friendRequestPutDTO.getStatus());
        }
        catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred:\n"+e.getMessage());
        }
        return DTOMapper.INSTANCE.convertEntityToFriendRequestGetDTO(updatedFriendRequest);
    }


}
