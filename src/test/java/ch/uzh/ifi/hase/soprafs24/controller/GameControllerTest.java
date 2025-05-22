package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.constant.errors.InvalidGameStatusException;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameInvitation;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.enums.InvitationStatus;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameInvitationPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameInvitationPutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameInvitationService;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import ch.uzh.ifi.hase.soprafs24.constant.errors.GameInvitationNotFoundException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.GameNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @MockBean
    private UserService userService;

    @MockBean
    private GameInvitationService gameInvitationService;

    @Test
    public void createGame_validInput_gameCreated() throws Exception {
        Game game = new Game();
        game.setId(1L);
        User user = new User();
        user.setId(12345L);
        game.setHost(user);

        GamePostDTO gamePostDTO = new GamePostDTO();
        GameGetDTO gameGetDTO = new GameGetDTO();
        gameGetDTO.setHost(DTOMapper.INSTANCE.convertEntityToUserGetDTO(user));


        given(gameService.createGame(any(User.class))).willReturn(game);
        given(userService.getUserByToken("Bearer test-token")).willReturn(Optional.of(user));

        MockHttpServletRequestBuilder postRequest = post("/games")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer test-token")
            .content(asJsonString(gamePostDTO));

        mockMvc.perform(postRequest)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id", is(game.getId().intValue())))
            .andExpect(jsonPath("$.host.id", is(user.getId().intValue())));

    }

    @Test
    public void createGame_invalidInput_runTimeException() throws Exception {
        GamePostDTO gamePostDTO = new GamePostDTO();

        given(userService.getUserByToken("Bearer test-token")).willReturn(Optional.empty());

        MockHttpServletRequestBuilder postRequest = post("/games")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer test-token")
            .content(asJsonString(gamePostDTO));

        mockMvc.perform(postRequest)
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createGame_userNotFoundException_returnsNotFound() throws Exception {
        given(userService.getUserByToken("token")).willReturn(Optional.of(new User()));
        doThrow(new UserNotFoundException("User not found")).when(gameService).createGame(any());

        mockMvc.perform(post("/games")
                .header("Authorization", "token"))
            .andExpect(status().isNotFound());
    }

    @Test
    void createGame_unexpectedException_returnsInternalServerError() throws Exception {
        given(userService.getUserByToken("token")).willReturn(Optional.of(new User()));
        doThrow(new RuntimeException("Unexpected")).when(gameService).createGame(any());

        mockMvc.perform(post("/games")
                .header("Authorization", "token"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    public void getGameById_validId_gameReturned() throws Exception {

        User user = new User();
        user.setId(12345L);
        Game game = new Game();
        game.setId(1L);
        game.setHost(user);


        given(gameService.getGameById(1L)).willReturn(Optional.of(game));

        mockMvc.perform(get("/games/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.host.id", is(user.getId().intValue())));
    }

    @Test
    public void getGameById_invalidId_notFound() throws Exception {
        given(gameService.getGameById(1L)).willReturn(Optional.empty());

        mockMvc.perform(get("/games/1"))
            .andExpect(status().isNotFound());
    }

    @Test
    public void updateGame_validGameState_gameUpdated() throws Exception {
        // Assign
        Game game = new Game();
        game.setId(1L);
        User user = new User();
        user.setId(12345L);
        game.setHost(user);
        game.setGameStatus(GameStatus.CREATED);

        GamePutDTO gamePutDTO = new GamePutDTO();
        gamePutDTO.setGameStatus(GameStatus.CREATED);

        given(gameService.getGameById(1L)).willReturn(Optional.of(game));
        given(userService.getUserByToken("Bearer test-token")).willReturn(Optional.of(user));
        given(gameService.isUserInGame(game, user)).willReturn(true);
        given(gameService.updateGameStatus(game, gamePutDTO.getGameStatus())).willReturn(game);

        MockHttpServletRequestBuilder putRequest = put("/games/1")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer test-token")
            .content(asJsonString(gamePutDTO));

        // Act & Assert
        mockMvc.perform(putRequest)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id", is(game.getId().intValue())))
            .andExpect(jsonPath("$.host.id", is(user.getId().intValue())))
            .andExpect(jsonPath("$.gameStatus", is(gamePutDTO.getGameStatus().toString())));
    }

    @Test
    public void updateGame_invalidGameState_gameNotUpdated() throws Exception {
        // Assign
        Game game = new Game();
        game.setId(1L);
        User user = new User();
        user.setId(12345L);
        game.setHost(user);

        GamePutDTO gamePutDTO = new GamePutDTO();
        gamePutDTO.setGameStatus(GameStatus.CREATED);

        given(gameService.getGameById(1L)).willReturn(Optional.of(game));
        given(userService.getUserByToken("Bearer test-token")).willReturn(Optional.of(user));
        given(gameService.isUserInGame(game, user)).willReturn(true);
        doThrow(new InvalidGameStatusException("Invalid game status transition message"))
            .when(gameService).updateGameStatus(game, gamePutDTO.getGameStatus());
        //given(gameService.updateGameStatus(game, gamePutDTO.getGameStatus())).willThrow(new InvalidGameStatusException("Invalid game status transition message"));

        MockHttpServletRequestBuilder putRequest = put("/games/1")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer test-token")
            .content(asJsonString(gamePutDTO));

        // Act & Assert
        ResultActions res = mockMvc.perform(putRequest)
            .andExpect(status().isConflict());
            //.andExpect(jsonPath("$.message", is("Invalid game status transition message")));
        // TODO: TEST RESPONSE ERROR MESSAGE
    }

    @Test
    void updateGame_unexpectedException_returnsBadRequest() throws Exception {
        User user = new User(); user.setId(1L);
        Game game = new Game(); game.setId(1L); game.addUser(user);

        GamePutDTO dto = new GamePutDTO();
        dto.setGameStatus(GameStatus.CREATED);

        given(userService.getUserByToken("token")).willReturn(Optional.of(user));
        given(gameService.getGameById(1L)).willReturn(Optional.of(game));
        given(gameService.isUserInGame(game, user)).willReturn(true);
        doThrow(new RuntimeException("Unexpected")).when(gameService).updateGameStatus(any(), any());

        mockMvc.perform(put("/games/1")
                .header("Authorization", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void createGameInvitation_validRequest_returnsCreated() throws Exception {
        User sender = new User(); sender.setId(1L);
        User target = new User(); target.setId(2L); target.setUsername("fasdfasdfa");
        Game game = new Game(); game.setId(1L);
        GameInvitation invitation = new GameInvitation();
        invitation.setId(1L); invitation.setSender(sender); invitation.setTarget(target); invitation.setGame(game); invitation.setStatus(InvitationStatus.PENDING);

        GameInvitationPostDTO dto = new GameInvitationPostDTO();
        dto.setTargetUsername(target.getUsername()); dto.setGameId(1L);

        when(userService.getUserByToken("token")).thenReturn(Optional.of(sender));
        when(userService.getUserByUsername(target.getUsername())).thenReturn(target);
        when(gameService.getGameById(1L)).thenReturn(Optional.of(game));
        when(gameInvitationService.createGameInvitation(any(), any(), any())).thenReturn(invitation);

        mockMvc.perform(post("/games/invitations")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.game.id").value(1L));
    }

    @Test
    void createGameInvitation_missingToken_returnsUnauthorized() throws Exception {
        when(userService.getUserByToken(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/games/invitations")
                        .header("Authorization", "fasdfad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(new GameInvitationPostDTO())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createGameInvitation_inviteSelf_returnsConflict() throws Exception {
        User sender = new User(); sender.setId(1L); sender.setUsername("fdasfasdfa");
        GameInvitationPostDTO dto = new GameInvitationPostDTO();
        dto.setTargetUsername(sender.getUsername()); dto.setGameId(1L);

        when(userService.getUserByToken("token")).thenReturn(Optional.of(sender));
        when(userService.getUserById(1L)).thenReturn(Optional.of(sender));
        when(userService.getUserByUsername(sender.getUsername())).thenReturn(sender);

        mockMvc.perform(post("/games/invitations")
                        .header("Authorization", "token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    void createGameInvitation_gameNotFound_returnsNotFound() throws Exception {
        User sender = new User(); sender.setId(1L);
        GameInvitationPostDTO dto = new GameInvitationPostDTO();
        dto.setTargetUsername("target");
        dto.setGameId(1L);

        given(userService.getUserByToken("token")).willReturn(Optional.of(sender));
        given(userService.getUserByUsername("target")).willReturn(new User());
        given(gameService.getGameById(1L)).willReturn(Optional.of(new Game()));
        doThrow(new GameNotFoundException("Game not found"))
            .when(gameInvitationService).createGameInvitation(any(), any(), any());

        mockMvc.perform(post("/games/invitations")
                .header("Authorization", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
            .andExpect(status().isNotFound());
    }

    @Test
    void createGameInvitation_illegalArgument_returnsConflict() throws Exception {
        User sender = new User(); sender.setId(1L);
        GameInvitationPostDTO dto = new GameInvitationPostDTO();
        dto.setTargetUsername("target");
        dto.setGameId(1L);

        given(userService.getUserByToken("token")).willReturn(Optional.of(sender));
        given(userService.getUserByUsername("target")).willReturn(new User());
        given(gameService.getGameById(1L)).willReturn(Optional.of(new Game()));
        doThrow(new IllegalArgumentException("Already invited"))
            .when(gameInvitationService).createGameInvitation(any(), any(), any());

        mockMvc.perform(post("/games/invitations")
                .header("Authorization", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
            .andExpect(status().isConflict());
    }

    @Test
    void createGameInvitation_unexpectedException_returnsInternalServerError() throws Exception {
        User sender = new User(); sender.setId(1L);
        GameInvitationPostDTO dto = new GameInvitationPostDTO();
        dto.setTargetUsername("target");
        dto.setGameId(1L);

        given(userService.getUserByToken("token")).willReturn(Optional.of(sender));
        given(userService.getUserByUsername("target")).willReturn(new User());
        given(gameService.getGameById(1L)).willReturn(Optional.of(new Game()));
        doThrow(new RuntimeException("Unexpected error"))
            .when(gameInvitationService).createGameInvitation(any(), any(), any());

        mockMvc.perform(post("/games/invitations")
                .header("Authorization", "token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(dto)))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void deleteGame_validRequest_gameDeleted() throws Exception {
    
    Game game = new Game();
    game.setId(1L);
    game.setUsers(new ArrayList<>());

    User user = new User();
    user.setId(12345L);

    given(userService.getUserByToken("test-token")).willReturn(Optional.of(user));
    given(gameService.getGameById(1L)).willReturn(Optional.of(game));

    mockMvc.perform(delete("/games/1")
            .header("Authorization", "test-token"))
        .andExpect(status().isNoContent());

    Mockito.verify(gameService).deleteGame(game);
    }

    @Test
    void deleteGame_gameNotFound_returnsNotFound() throws Exception {
    
        given(userService.getUserByToken("test-token")).willReturn(Optional.of(new User()));
        given(gameService.getGameById(1L)).willReturn(Optional.empty());

        mockMvc.perform(delete("/games/1")
                .header("Authorization", "test-token"))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteGame_usersInGame_returnsConflict() throws Exception {
    
        Game game = new Game();
        game.setId(1L);

        User user1 = new User();
        user1.setId(12345L);
        User user2 = new User();
        user2.setId(67890L);

        List<User> users = new ArrayList<>();
        users.add(user1);
        users.add(user2);
        game.setUsers(users);

        given(userService.getUserByToken("test-token")).willReturn(Optional.of(user1));
        given(gameService.getGameById(1L)).willReturn(Optional.of(game));

        mockMvc.perform(delete("/games/1")
                .header("Authorization", "test-token"))
            .andExpect(status().isConflict());
    }

    @Test
    void deleteGame_invalidToken_returnsUnauthorized() throws Exception {

        given(userService.getUserByToken("invalid-token")).willReturn(Optional.empty());

        mockMvc.perform(delete("/games/1")
                        .header("Authorization", "invalid-token"))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void getRemainingLetters_validRequestOpponentNoLetters_returnsRemainingLetters() throws Exception {
        // Arrange
        Game game = new Game();
        game.setId(1L);
        game.setTilesForPlayer(12345L, List.of("A", "B", "C", "D"));

        User user = new User(); user.setId(2L);
        User opponent = new User(); opponent.setId(12345L);
        game.addUser(opponent); game.addUser(user);

        given(gameService.getGameById(1L)).willReturn(Optional.of(game));
        given(userService.getUserByToken("Bearer test-token")).willReturn(Optional.of(user));
        given(gameService.countLettersInBag(game, 'O')).willReturn(8);

        // Act & Assert
        mockMvc.perform(get("/games/1/letters/O")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(8)));
    }

    @Test
    void getRemainingLetters_invalidGameId_returnsNotFound() throws Exception {
        // Arrange
        given(gameService.getGameById(1L)).willReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/games/1/letters/O")
                    .header("Authorization", "Bearer test-token"))
                    .andExpect(status().isNotFound())
                    .andExpect(status().reason(is("Game not found")));
    }

    @Test
    void getRemainingLetters_invalidToken_returnsUnauthorized() throws Exception {
        // Arrange
        Game game = new Game();
        given(gameService.getGameById(1L)).willReturn(Optional.of(game));
        given(userService.getUserByToken("invalid-token")).willReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/games/1/letters/O")
                .header("Authorization", "invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(containsString("Invalid token: User not found")));
    }

    @Test
    void getRemainingLetters_validRequestOpponentHasLetters_returnsCorrectValue() throws Exception {
        // Arrange
        Game game = new Game();
        game.setId(1L);
        game.setTilesForPlayer(12345L, List.of("A", "B", "A", "D"));

        User user = new User(); user.setId(2L);
        User opponent = new User(); opponent.setId(12345L);
        game.addUser(user); game.addUser(opponent);

        given(gameService.getGameById(1L)).willReturn(Optional.of(game));
        given(userService.getUserByToken("Bearer test-token")).willReturn(Optional.of(user));
        given(gameService.countLettersInBag(game, 'A')).willReturn(5);

        // Act & Assert
        mockMvc.perform(get("/games/1/letters/A")
                .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", is(7)));
    }

    @Test
    void updateGame_unauthorized_returns401() throws Exception {
        GamePutDTO gamePutDTO = new GamePutDTO();
        gamePutDTO.setGameStatus(GameStatus.CREATED);

        given(userService.getUserByToken("Bearer test-token")).willReturn(Optional.empty());

        MockHttpServletRequestBuilder putRequest = put("/games/1")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer test-token")
            .content(asJsonString(gamePutDTO));

        mockMvc.perform(putRequest)
            .andExpect(status().isUnauthorized());
    }


    @Test
    void getGameInvitations_noInvitations_returnsEmptyList() throws Exception {
        User user = new User();
        user.setId(1L);

        given(userService.getUserByToken("test-token")).willReturn(Optional.of(user));
        given(gameInvitationService.getGameInvitationsByTarget(user)).willReturn(List.of());

        mockMvc.perform(get("/games/invitations/1")
                .header("Authorization", "test-token"))
            .andExpect(status().isOk())
            .andExpect(content().json("[]"));
    }

    @Test
    void updateGameInvitation_invitationNotFound_returnsNotFound() throws Exception {
        User user = new User();
        user.setId(1L);

        GameInvitationPutDTO dto = new GameInvitationPutDTO();
        dto.setStatus(InvitationStatus.ACCEPTED);

        given(userService.getUserByToken("test-token")).willReturn(Optional.of(user));
        given(gameInvitationService.getGameInvitationById(anyLong()))
            .willThrow(new GameInvitationNotFoundException("not found"));

        mockMvc.perform(put("/games/invitations/1")
                .header("Authorization", "test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateGameInvitation_missingToken_returnsUnauthorized() throws Exception {
        GameInvitationPutDTO dto = new GameInvitationPutDTO();
        dto.setStatus(InvitationStatus.ACCEPTED);

        given(userService.getUserByToken("")).willReturn(Optional.empty());

        mockMvc.perform(put("/games/invitations/1")
                .header("Authorization", "")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void updateGameInvitation_nullInvitationId_returnsBadRequest() throws Exception {
        User user = new User(); user.setId(1L);
        GameInvitationPutDTO dto = new GameInvitationPutDTO();
        dto.setStatus(InvitationStatus.ACCEPTED);

        given(userService.getUserByToken("test-token")).willReturn(Optional.of(user));

        mockMvc.perform(put("/games/invitations/")
                .header("Authorization", "test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
            .andExpect(status().isBadRequest()); // No path variable, so 404 from Spring
        // To test empty string, you would need to call with "/games/invitations/" which is not a valid endpoint.
    }

    @Test
    void updateGameInvitation_nullStatus_returnsBadRequest() throws Exception {
        User user = new User(); user.setId(1L);
        GameInvitationPutDTO dto = new GameInvitationPutDTO(); // status is null

        given(userService.getUserByToken("test-token")).willReturn(Optional.of(user));

        mockMvc.perform(put("/games/invitations/1")
                .header("Authorization", "test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void updateGameInvitation_gameNotFound_returnsNotFound() throws Exception {
        User user = new User(); user.setId(1L);
        GameInvitation invitation = new GameInvitation(); invitation.setId(1L); invitation.setTarget(user);

        GameInvitationPutDTO dto = new GameInvitationPutDTO();
        dto.setStatus(InvitationStatus.ACCEPTED);

        given(userService.getUserByToken("test-token")).willReturn(Optional.of(user));
        given(gameInvitationService.getGameInvitationById(1L)).willReturn(invitation);
        doThrow(new GameNotFoundException("Game not found")).when(gameInvitationService).updateGameInvitationStatus(any(), any());

        mockMvc.perform(put("/games/invitations/1")
                .header("Authorization", "test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateGameInvitation_userNotFound_returnsNotFound() throws Exception {
        User user = new User(); user.setId(1L);
        GameInvitation invitation = new GameInvitation(); invitation.setId(1L); invitation.setTarget(user);

        GameInvitationPutDTO dto = new GameInvitationPutDTO();
        dto.setStatus(InvitationStatus.ACCEPTED);

        given(userService.getUserByToken("test-token")).willReturn(Optional.of(user));
        given(gameInvitationService.getGameInvitationById(1L)).willReturn(invitation);
        doThrow(new UserNotFoundException("User not found")).when(gameInvitationService).updateGameInvitationStatus(any(), any());

        mockMvc.perform(put("/games/invitations/1")
                .header("Authorization", "test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateGameInvitation_illegalArgument_returnsConflict() throws Exception {
        User user = new User(); user.setId(1L);
        GameInvitation invitation = new GameInvitation(); invitation.setId(1L); invitation.setTarget(user);

        GameInvitationPutDTO dto = new GameInvitationPutDTO();
        dto.setStatus(InvitationStatus.ACCEPTED);

        given(userService.getUserByToken("test-token")).willReturn(Optional.of(user));
        given(gameInvitationService.getGameInvitationById(1L)).willReturn(invitation);
        doThrow(new IllegalArgumentException("Game is already full")).when(gameInvitationService).updateGameInvitationStatus(any(), any());

        mockMvc.perform(put("/games/invitations/1")
                .header("Authorization", "test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
            .andExpect(status().isConflict());
    }

    @Test
    void updateGameInvitation_unexpectedException_returnsInternalServerError() throws Exception {
        User user = new User(); user.setId(1L);
        GameInvitation invitation = new GameInvitation(); invitation.setId(1L); invitation.setTarget(user);

        GameInvitationPutDTO dto = new GameInvitationPutDTO();
        dto.setStatus(InvitationStatus.ACCEPTED);

        given(userService.getUserByToken("test-token")).willReturn(Optional.of(user));
        given(gameInvitationService.getGameInvitationById(1L)).willReturn(invitation);
        doThrow(new RuntimeException("Unexpected error")).when(gameInvitationService).updateGameInvitationStatus(any(), any());

        mockMvc.perform(put("/games/invitations/1")
                .header("Authorization", "test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void updateGameInvitation_success_returnsOk() throws Exception {
        User user = new User(); user.setId(1L);
        GameInvitation invitation = new GameInvitation(); invitation.setId(1L); invitation.setTarget(user);

        GameInvitationPutDTO dto = new GameInvitationPutDTO();
        dto.setStatus(InvitationStatus.ACCEPTED);

        given(userService.getUserByToken("test-token")).willReturn(Optional.of(user));
        given(gameInvitationService.getGameInvitationById(1L)).willReturn(invitation);
        // No exception thrown
        // Optionally, you can mock the return value of updateGameInvitationStatus if needed

        mockMvc.perform(put("/games/invitations/1")
                .header("Authorization", "test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(dto)))
            .andExpect(status().isOk());
    }   

    @Test
    void leaveGame_success() throws Exception {
        Game game = new Game();
        game.setId(1L);
        User user = new User();
        user.setId(123L);
        game.addUser(user);

        given(userService.getUserByToken("token")).willReturn(Optional.of(user));
        given(gameService.getGameById(1L)).willReturn(Optional.of(game));
        given(gameService.isUserInGame(game, user)).willReturn(true);

        mockMvc.perform(put("/games/1/users/123/leave")
                .header("Authorization", "token"))
            .andExpect(status().isOk());
    }

    @Test
    void leaveGame_invalidToken_returnsUnauthorized() throws Exception {
        given(userService.getUserByToken("token")).willReturn(Optional.empty());

        mockMvc.perform(put("/games/1/users/123/leave")
                .header("Authorization", "token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void leaveGame_userNotInGame_returnsUnauthorized() throws Exception {
        Game game = new Game();
        game.setId(1L);
        User user = new User();
        user.setId(123L);

        given(userService.getUserByToken("token")).willReturn(Optional.of(user));
        given(gameService.getGameById(1L)).willReturn(Optional.of(game));
        given(gameService.isUserInGame(game, user)).willReturn(false);

        mockMvc.perform(put("/games/1/users/123/leave")
                .header("Authorization", "token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void leaveGame_gameNotFound_returnsNotFound() throws Exception {
        User user = new User();
        user.setId(123L);

        given(userService.getUserByToken("token")).willReturn(Optional.of(user));
        given(gameService.getGameById(1L)).willReturn(Optional.empty());

        mockMvc.perform(put("/games/1/users/123/leave")
                .header("Authorization", "token"))
            .andExpect(status().isNotFound());
    }

    @Test
    void leaveGame_userNotFoundException_returnsNotFound() throws Exception {
        Game game = new Game();
        game.setId(1L);
        User user = new User();
        user.setId(123L);
        game.addUser(user);

        given(userService.getUserByToken("token")).willReturn(Optional.of(user));
        given(gameService.getGameById(1L)).willReturn(Optional.of(game));
        given(gameService.isUserInGame(game, user)).willReturn(true);
        doThrow(new UserNotFoundException("User not found")).when(gameService).leaveGame(game, user);

        mockMvc.perform(put("/games/1/users/123/leave")
                .header("Authorization", "token"))
            .andExpect(status().isNotFound());
    }

    @Test
    void leaveGame_unexpectedException_returnsInternalServerError() throws Exception {
        Game game = new Game();
        game.setId(1L);
        User user = new User();
        user.setId(123L);
        game.addUser(user);

        given(userService.getUserByToken("token")).willReturn(Optional.of(user));
        given(gameService.getGameById(1L)).willReturn(Optional.of(game));
        given(gameService.isUserInGame(game, user)).willReturn(true);
        doThrow(new RuntimeException("Unexpected")).when(gameService).leaveGame(game, user);

        mockMvc.perform(put("/games/1/users/123/leave")
                .header("Authorization", "token"))
            .andExpect(status().isInternalServerError());
    }

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}
