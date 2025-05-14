package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.constant.errors.GameNotFoundException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.InvalidGameStatusException;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GameService gameService;

    private Game testGame;
    private User testHost;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Create test host
        testHost = new User();
        testHost.setId(1L);
        testHost.setUsername("host");
        testHost.setStatus(UserStatus.ONLINE);

        // Create test game
        testGame = new Game();
        testGame.setId(2L);
        testGame.setHost(testHost);
        testGame.setUsers(List.of(testHost));
        testGame.setStartTime(LocalDateTime.now());
        testGame.setGameStatus(GameStatus.CREATED);
    }

    @Test
    public void createGame_validInput_gameCreated() throws UserNotFoundException {
        // Arrange
        when(userRepository.findById(testHost.getId())).thenReturn(Optional.of(testHost));
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);

        // Act
        Game createdGame = gameService.createGame(testHost);

        // Assert
        assertNotNull(createdGame);
        assertEquals(testGame.getId(), createdGame.getId());
        assertEquals(testHost.getId(), createdGame.getHost().getId());
        assertEquals(UserStatus.IN_GAME, createdGame.getHost().getStatus());
        assertEquals(true, createdGame.getHost().isInGame());
        verify(gameRepository, times(1)).save(any(Game.class));
    }

    @Test
    public void updateGameStatus_validInput_gameStatusUpdated() throws GameNotFoundException {
        // Arrange
        Game updatedGameData = testGame;
        updatedGameData.setGameStatus(GameStatus.ONGOING);
        when(gameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
        when(gameRepository.saveAndFlush(testGame)).thenReturn(updatedGameData);

        // Act
        Game updatedGame = gameService.updateGameStatus(testGame, GameStatus.ONGOING);

        // Assert
        assertNotNull(updatedGame);
        assertEquals(GameStatus.ONGOING, updatedGame.getGameStatus());
        verify(gameRepository, times(1)).findById(testGame.getId());
    }

    @Test
    public void updateGameStatus_invalidGame_gameNotFound() {
        // Arrange
        when(gameRepository.findById(testGame.getId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(GameNotFoundException.class, () -> gameService.updateGameStatus(testGame, GameStatus.ONGOING));
        verify(gameRepository, times(1)).findById(testGame.getId());
    }
    @Test
    public void updateGameStatus_invalidTransition_throwsException() {
        // Arrange
        Game game = new Game();
        game.setId(1L);
        game.setGameStatus(GameStatus.TERMINATED);

        when(gameRepository.findById(game.getId())).thenReturn(Optional.of(game));

        // Act & Assert
        InvalidGameStatusException exception = assertThrows(InvalidGameStatusException.class, () -> {
        gameService.updateGameStatus(game, GameStatus.ONGOING);
        });

        assertEquals("Invalid game status transition from TERMINATED to ONGOING", exception.getMessage());
        verify(gameRepository, times(1)).findById(game.getId());
    }

    @Test
    public void getGameById_validId_gameReturned() {
        // Arrange
        when(gameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));

        // Act
        Optional<Game> foundGame = gameService.getGameById(testGame.getId());

        // Assert
        assertNotNull(foundGame);
        assertTrue(foundGame.isPresent());
        assertEquals(testGame.getId(), foundGame.get().getId());
        verify(gameRepository, times(1)).findById(testGame.getId());
    }

    @Test
    public void getGameById_invalidId_gameNotFound() {
        // Arrange
        when(gameRepository.findById(testGame.getId())).thenReturn(Optional.empty());

        // Act & Assert
        assertTrue(gameService.getGameById(testGame.getId()).isEmpty());
        verify(gameRepository, times(1)).findById(testGame.getId());
    }

    @Test
    public void deleteGame_validGame_gameDeleted() {

    Game game = new Game();
    game.setId(1L);
    gameService.deleteGame(game);

    verify(gameRepository, times(1)).delete(game);
}

    @Test
    void testAssignLetters() {
        // Arrange
        when(gameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
        when(gameRepository.saveAndFlush(testGame)).thenReturn(testGame);
        testGame.setTilesForPlayer(testHost.getId(), List.of("A", "B", "C", "D"));

        // Act
        String[] assignedLetters = gameService.assignNewLetters(testGame, testHost.getId(), testGame.getPlayerTiles(testHost.getId()));
        List<String> assignedLettersList = Arrays.asList(assignedLetters);

        // Assert
        assertNotNull(assignedLetters);
        assertEquals(7, assignedLetters.length);
        assertTrue(assignedLettersList.contains("A"));
        assertTrue(assignedLettersList.contains("B"));
        assertTrue(assignedLettersList.contains("C"));
        assertTrue(assignedLettersList.contains("D"));
    }

    @Test
    void testAssignLettersOutOfBounds() {
        // Arrange
        when(gameRepository.findById(testGame.getId())).thenReturn(Optional.of(testGame));
        when(gameRepository.saveAndFlush(testGame)).thenReturn(testGame);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            gameService.assignNewLetters(testGame, testHost.getId(), new String[]{"A", "B", "C", "D", "E", "F", "G", "H"});
        });
    }

}