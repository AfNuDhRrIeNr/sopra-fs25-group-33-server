package ch.uzh.ifi.hase.soprafs24.rest.mapper;

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
  UserGetDTO convertEntityToUserGetDTO(User user);


  @Mapping(source = "users", target = "users")
  @Mapping(source = "board", target = "board")
  @Mapping(source = "host", target = "host")
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
    @Mapping(target = "status", expression = "java(InvitationStatus.valueOf(gameInvitationPutDTO.getStatus()))")
    void updateGameInvitationFromPutDTO(GameInvitationPutDTO gameInvitationPutDTO, @MappingTarget GameInvitation gameInvitation);

    // Note: We assume Game and Target are set manually in service layer using gameId and targetId from PostDTO.
    // So no @Mapping is needed here, but this can act as a base converter if needed.
    default GameInvitation convertGameInvitationPostDTOtoEntity(GameInvitationPostDTO postDTO, Game game, User sender, User target) {
        GameInvitation invitation = new GameInvitation();
        invitation.setGame(game);
        invitation.setSender(sender);
        invitation.setTarget(target);
        invitation.setStatus(InvitationStatus.PENDING);
        invitation.setTimeStamp(java.time.LocalDateTime.now());
        return invitation;
    }
}
