package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.errors.GameNotFoundException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameInvitation;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.enums.InvitationStatus;
import ch.uzh.ifi.hase.soprafs24.repository.GameInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

class GameInvitationServiceTest {

    @InjectMocks
    private GameInvitationService gameInvitationService;

    @InjectMocks
    private GameService gameService;

    @InjectMocks
    private UserService userService;

    @Mock
    private GameInvitationRepository gameInvitationRepository;

    @BeforeEach
    void setup() {
        gameService = Mockito.mock(GameService.class);
        userService = Mockito.mock(UserService.class);
        gameInvitationRepository = Mockito.mock(GameInvitationRepository.class);
        gameInvitationService = new GameInvitationService(gameService, userService, gameInvitationRepository);
    }

    @Test
    void createGameInvitation_validData_success() throws Exception {
        // Assign
        Game game = new Game(); game.setId(1L);
        User sender = new User(); sender.setId(1L);
        User target = new User(); target.setId(2L);

        Mockito.when(gameService.getGameById(1L)).thenReturn(Optional.of(game));
        Mockito.when(userService.getUserById(1L)).thenReturn(Optional.of(sender));
        Mockito.when(userService.getUserById(2L)).thenReturn(Optional.of(target));
        Mockito.when(gameInvitationRepository.findByGameAndTarget(game, target)).thenReturn(Optional.empty());

        GameInvitation result = new GameInvitation();
        result.setGame(game);
        result.setSender(sender);
        result.setTarget(target);

        Mockito.when(gameInvitationRepository.saveAndFlush(any())).thenReturn(result);

        // Act
        GameInvitation created = gameInvitationService.createGameInvitation(Optional.of(game), Optional.of(sender), Optional.of(target));

        // Assert
        assertEquals(game, created.getGame());
        assertEquals(sender, created.getSender());
        assertEquals(target, created.getTarget());
    }

    @Test
    void createGameInvitation_gameNotFound_throwsException() {
        // Assign
        Game game = new Game(); game.setId(1L);
        User sender = new User(); sender.setId(1L);
        User target = new User(); target.setId(2L);

        Mockito.when(gameService.getGameById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(GameNotFoundException.class, () ->
                gameInvitationService.createGameInvitation(Optional.of(game), Optional.of(sender), Optional.of(target)));
    }

    @Test
    void createGameInvitation_userNotFound_throwsException() {
        // Assign
        Game game = new Game(); game.setId(1L);
        User sender = new User(); sender.setId(1L);
        User target = new User(); target.setId(2L);

        Mockito.when(gameService.getGameById(1L)).thenReturn(Optional.of(game));
        Mockito.when(userService.getUserById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () ->
                gameInvitationService.createGameInvitation(Optional.of(game), Optional.of(sender), Optional.of(target)));
    }

    @Test
    void createGameInvitation_inviteAlreadyExists_throwsException() {
        // Assign
        Game game = new Game(); game.setId(1L);
        User sender = new User(); sender.setId(1L);
        User target = new User(); target.setId(2L);

        Mockito.when(gameService.getGameById(1L)).thenReturn(Optional.of(game));
        Mockito.when(userService.getUserById(1L)).thenReturn(Optional.of(sender));
        Mockito.when(userService.getUserById(2L)).thenReturn(Optional.of(target));
        Mockito.when(gameInvitationRepository.findByGameAndTarget(game, target)).thenReturn(Optional.of(new GameInvitation()));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                gameInvitationService.createGameInvitation(Optional.of(game), Optional.of(sender), Optional.of(target)));
    }

    @Test
    void updateGameInvitation_gameAlreadyFull_throwsException() throws UserNotFoundException, GameNotFoundException {
        // Assign
        Game game = new Game(); game.setId(1L);
        User sender = new User(); sender.setId(1L);
        User target = new User(); target.setId(2L);
        User userInGame = new User(); userInGame.setId(3L);
        game.setUsers(List.of(sender, userInGame));

        GameInvitation gameInvitation = new GameInvitation();
        gameInvitation.setId(1L);
        gameInvitation.setGame(game);
        gameInvitation.setSender(sender);
        gameInvitation.setTarget(target);

        Mockito.when(gameInvitationRepository.findById(1L)).thenReturn(Optional.of(gameInvitation));
        Mockito.when(gameService.getGameById(1L)).thenReturn(Optional.of(game));
        Mockito.when(userService.getUserById(1L)).thenReturn(Optional.of(sender));
        Mockito.when(userService.getUserById(2L)).thenReturn(Optional.of(target));

        // Act & Assert
         Exception exception = assertThrows(IllegalArgumentException.class, () ->
                gameInvitationService.updateGameInvitationStatus(gameInvitation, InvitationStatus.ACCEPTED));
         assertEquals("Game is already full", exception.getMessage());
         Mockito.verify(gameInvitationRepository, Mockito.never()).saveAndFlush(any());
         Mockito.verify(userService, Mockito.never()).updateUserStatus(any(), any());
         Mockito.verify(gameService, Mockito.never()).joinGame(any(), any());
    }
}
