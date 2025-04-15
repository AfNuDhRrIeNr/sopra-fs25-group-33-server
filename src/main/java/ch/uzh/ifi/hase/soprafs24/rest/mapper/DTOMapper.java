package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.FriendRequest;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameInvitation;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.enums.InvitationStatus;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

import java.util.LinkedList;
import java.util.Set;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper(imports = {LinkedList.class, InvitationStatus.class})
public interface DTOMapper {

  DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

  @Mapping(source = "password", target = "password")
  @Mapping(source = "username", target = "username")
  User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

  @Mapping(source = "id", target = "id")
  @Mapping(source = "username", target = "username")
  @Mapping(source = "status", target = "status")
  @Mapping(source = "token", target = "token")
  @Mapping(source = "inGame", target = "inGame")
  @Mapping(source = "highScore", target = "highScore")
  @Mapping(source = "friends", target = "friends", qualifiedByName = "convertFriendsToUserGetDTO")
  UserGetDTO convertEntityToUserGetDTO(User user);


  @Mapping(source = "users", target = "users", qualifiedByName = "convertEntityToUserGetDTO")
  @Mapping(source = "board", target = "board")
  @Mapping(source = "host", target = "host", qualifiedByName = "convertEntityToUserGetDTO")
  @Mapping(source = "gameStatus", target = "gameStatus")
  @Mapping(source = "hostTurn", target = "hostTurn") // Needs to be hostTurn instead of IsHostTurn (weird JPA internal implementation)
  @Mapping(source = "startTime", target = "startTime")
  GameGetDTO convertEntityToGameGetDTO(Game game);


    // --- GameInvitation -> GameInvitationsGetDTO ---
    @Mapping(source = "id", target = "id")
    @Mapping(source = "sender", target = "sender", qualifiedByName = "convertEntityToUserGetDTO")
    @Mapping(source = "target", target = "target", qualifiedByName = "convertEntityToUserGetDTO")
    @Mapping(source = "game", target = "game", qualifiedByName = "convertEntityToGameGetDTO")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "timeStamp", target = "timeStamp")
    GameInvitationsGetDTO convertEntityToGameInvitationsGetDTO(GameInvitation gameInvitation);

    // --- GameInvitationPutDTO -> GameInvitation ---
    @Mapping(target = "status", source = "status")
    void updateGameInvitationFromPutDTO(GameInvitationPutDTO gameInvitationPutDTO, @MappingTarget GameInvitation gameInvitation);

    @Mapping(source = "id", target = "id")
    @Mapping(source = "sender", target = "sender", qualifiedByName = "convertEntityToUserGetDTO")
    @Mapping(source = "target", target = "target", qualifiedByName = "convertEntityToUserGetDTO")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "timeStamp", target = "timeStamp")
    @Mapping(source = "message", target = "message")
    FriendRequestGetDTO convertEntityToFriendRequestGetDTO(FriendRequest friendRequest);

    default String[] convertFriendsToUserGetDTOWithoutFriends(Set<User> friends) {
           return friends.stream()
                .map(User::getUsername)
                .toArray(String[]::new);
    }
}
