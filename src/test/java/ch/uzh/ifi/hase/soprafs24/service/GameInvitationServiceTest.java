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

    @Test
    void createGameInvitation_invitationAlreadyExists_reuseInvitation() throws UserNotFoundException, GameNotFoundException {
        // Assign
        Game game = new Game(); game.setId(1L);
        User sender = new User(); sender.setId(1L);
        User target = new User(); target.setId(2L);

        Mockito.when(gameService.getGameById(1L)).thenReturn(Optional.of(game));
        Mockito.when(userService.getUserById(1L)).thenReturn(Optional.of(sender));
        Mockito.when(userService.getUserById(2L)).thenReturn(Optional.of(target));

        GameInvitation existingInvitation = new GameInvitation();
        existingInvitation.setId(1L);
        existingInvitation.setGame(game);
        existingInvitation.setSender(sender);
        existingInvitation.setTarget(target);
        existingInvitation.setStatus(InvitationStatus.PENDING);

        Mockito.when(gameInvitationRepository.findByGameAndTarget(game, target)).thenReturn(Optional.of(existingInvitation));
        Mockito.when(gameInvitationRepository.saveAndFlush(Mockito.any())).thenReturn(existingInvitation);

        // Act
        GameInvitation created = gameInvitationService.createGameInvitation(Optional.of(game), Optional.of(sender), Optional.of(target));

        // Assert
        assertEquals(existingInvitation.getId(), created.getId());
        assertEquals(InvitationStatus.PENDING, created.getStatus());
        Mockito.verify(gameInvitationRepository, Mockito.times(1)).saveAndFlush(existingInvitation);
        Mockito.verify(gameInvitationRepository,Mockito.atLeastOnce()).findByGameAndTarget(game, target);
    }

    @Test
    void getGameInvitationById_validId_returnsInvitation() throws Exception {
        GameInvitation invitation = new GameInvitation();
        invitation.setId(1L);
        Mockito.when(gameInvitationRepository.findById(1L)).thenReturn(Optional.of(invitation));

        GameInvitation result = gameInvitationService.getGameInvitationById(1L);

        assertEquals(invitation, result);
    }

    @Test
    void getGameInvitationById_nullId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> gameInvitationService.getGameInvitationById(null));
    }

    @Test
    void getGameInvitationById_notFound_throwsException() {
        Mockito.when(gameInvitationRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ch.uzh.ifi.hase.soprafs24.constant.errors.GameInvitationNotFoundException.class,
            () -> gameInvitationService.getGameInvitationById(1L));
    }

    @Test
    void getGameInvitationsByTarget_validUser_returnsList() {
        User user = new User(); user.setId(1L);
        List<GameInvitation> invitations = List.of(new GameInvitation());
        Mockito.when(gameInvitationRepository.findAllByTarget(user)).thenReturn(invitations);

        List<GameInvitation> result = gameInvitationService.getGameInvitationsByTarget(user);

        assertEquals(invitations, result);
    }

    @Test
    void getGameInvitationsByTarget_nullUser_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> gameInvitationService.getGameInvitationsByTarget(null));
    }

    @Test
    void getGameInvitationsByTarget_nullUserId_throwsException() {
        User user = new User(); user.setId(null);
        assertThrows(IllegalArgumentException.class, () -> gameInvitationService.getGameInvitationsByTarget(user));
    }

    @Test
    void updateGameInvitationStatus_nullInvitation_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> gameInvitationService.updateGameInvitationStatus(null, InvitationStatus.ACCEPTED));
    }

    @Test
    void updateGameInvitationStatus_nullStatus_throwsException() {
        GameInvitation invitation = new GameInvitation(); invitation.setId(1L);
        assertThrows(IllegalArgumentException.class, () -> gameInvitationService.updateGameInvitationStatus(invitation, null));
    }

    @Test
    void updateGameInvitationStatus_gameNotFound_throwsException() {
        GameInvitation invitation = new GameInvitation(); invitation.setId(1L);
        invitation.setGame(new Game()); invitation.getGame().setId(1L);
        Mockito.when(gameService.getGameById(1L)).thenReturn(Optional.empty());
        assertThrows(GameNotFoundException.class, () -> gameInvitationService.updateGameInvitationStatus(invitation, InvitationStatus.ACCEPTED));
    }

    @Test
    void updateGameInvitationStatus_senderNotFound_throwsException() {
        Game game = new Game(); game.setId(1L);
        User sender = new User(); sender.setId(2L);
        GameInvitation invitation = new GameInvitation(); invitation.setId(1L);
        invitation.setGame(game); invitation.setSender(sender);
        Mockito.when(gameService.getGameById(1L)).thenReturn(Optional.of(game));
        Mockito.when(userService.getUserById(2L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> gameInvitationService.updateGameInvitationStatus(invitation, InvitationStatus.ACCEPTED));
    }

    @Test
    void updateGameInvitationStatus_targetNotFound_throwsException() {
        Game game = new Game(); game.setId(1L);
        User sender = new User(); sender.setId(2L);
        User target = new User(); target.setId(3L);
        GameInvitation invitation = new GameInvitation(); invitation.setId(1L);
        invitation.setGame(game); invitation.setSender(sender); invitation.setTarget(target);
        Mockito.when(gameService.getGameById(1L)).thenReturn(Optional.of(game));
        Mockito.when(userService.getUserById(2L)).thenReturn(Optional.of(sender));
        Mockito.when(userService.getUserById(3L)).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> gameInvitationService.updateGameInvitationStatus(invitation, InvitationStatus.ACCEPTED));
    }

    @Test
    void updateGameInvitationStatus_gameFull_throwsException() {
        Game game = new Game(); game.setId(1L);
        game.setUsers(List.of(new User(), new User()));
        User sender = new User(); sender.setId(2L);
        User target = new User(); target.setId(3L);
        GameInvitation invitation = new GameInvitation(); invitation.setId(1L);
        invitation.setGame(game); invitation.setSender(sender); invitation.setTarget(target);
        Mockito.when(gameService.getGameById(1L)).thenReturn(Optional.of(game));
        Mockito.when(userService.getUserById(2L)).thenReturn(Optional.of(sender));
        Mockito.when(userService.getUserById(3L)).thenReturn(Optional.of(target));
        assertThrows(IllegalArgumentException.class, () -> gameInvitationService.updateGameInvitationStatus(invitation, InvitationStatus.ACCEPTED));
    }

    @Test
    void updateGameInvitationStatus_statusAccepted_success() throws Exception {
        Game game = new Game(); game.setId(1L);
        game.setUsers(new java.util.ArrayList<>());
        User sender = new User(); sender.setId(2L);
        User target = new User(); target.setId(3L);
        GameInvitation invitation = new GameInvitation(); invitation.setId(1L);
        invitation.setGame(game); invitation.setSender(sender); invitation.setTarget(target);
        Mockito.when(gameService.getGameById(1L)).thenReturn(Optional.of(game));
        Mockito.when(userService.getUserById(2L)).thenReturn(Optional.of(sender));
        Mockito.when(userService.getUserById(3L)).thenReturn(Optional.of(target));
        Mockito.when(gameInvitationRepository.saveAndFlush(invitation)).thenReturn(invitation);

        GameInvitation result = gameInvitationService.updateGameInvitationStatus(invitation, InvitationStatus.ACCEPTED);

        assertEquals(InvitationStatus.ACCEPTED, result.getStatus());
        Mockito.verify(userService).updateUserStatus(target, ch.uzh.ifi.hase.soprafs24.constant.UserStatus.IN_GAME);
        Mockito.verify(gameService).joinGame(game, target);
        Mockito.verify(gameInvitationRepository).saveAndFlush(invitation);
    }

    @Test
    void updateGameInvitationStatus_statusPending_success() throws Exception {
        Game game = new Game(); game.setId(1L);
        game.setUsers(new java.util.ArrayList<>());
        User sender = new User(); sender.setId(2L);
        User target = new User(); target.setId(3L);
        GameInvitation invitation = new GameInvitation(); invitation.setId(1L);
        invitation.setGame(game); invitation.setSender(sender); invitation.setTarget(target);
        Mockito.when(gameService.getGameById(1L)).thenReturn(Optional.of(game));
        Mockito.when(userService.getUserById(2L)).thenReturn(Optional.of(sender));
        Mockito.when(userService.getUserById(3L)).thenReturn(Optional.of(target));
        Mockito.when(gameInvitationRepository.saveAndFlush(invitation)).thenReturn(invitation);

        GameInvitation result = gameInvitationService.updateGameInvitationStatus(invitation, InvitationStatus.PENDING);

        assertEquals(InvitationStatus.PENDING, result.getStatus());
        Mockito.verify(gameInvitationRepository).saveAndFlush(invitation);
    }
}
