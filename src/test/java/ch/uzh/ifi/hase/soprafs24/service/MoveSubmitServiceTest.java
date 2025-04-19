package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MoveSubmitServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private MoveValidatorService moveValidatorService;

    @InjectMocks
    @Spy
    private MoveSubmitService moveSubmitService;

    private static final int BOARD_SIZE = 15;
    private Game testGame;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        // Create test game with empty board
        testGame = new Game();
        testGame.setId(1L);
        testGame.setBoard(createEmptyBoard());

        // Set up mock repository
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        when(gameRepository.save(any(Game.class))).thenReturn(testGame);
    }

    private String[][] createEmptyBoard() {
        String[][] board = new String[BOARD_SIZE][BOARD_SIZE];
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = "";
            }
        }
        return board;
    }


    @Test
    void TestFirstWordScore() {
        // Use the real MoveValidatorService
        moveSubmitService = new MoveSubmitService(gameRepository, new MoveValidatorService(gameRepository));

        // Create an empty board for the initial state
        String[][] currentBoard = createEmptyBoard();
        testGame.setBoard(currentBoard);
        gameRepository.saveAndFlush(testGame);

        // Create a new board with the first word "HELLO" placed horizontally
        String[][] newBoard = createEmptyBoard();
        newBoard[7][7] = "H";
        newBoard[7][8] = "E";
        newBoard[7][9] = "L";
        newBoard[7][10] = "L";
        newBoard[7][11] = "O";

        // Execute the submitMove method
        int score = moveSubmitService.submitMove(1L, newBoard);

        // Verify the words formed
        List<String> expectedWords = List.of("HELLO");
        List<String> actualWords = new MoveValidatorService(gameRepository).findWords(currentBoard, newBoard);

        System.out.println("Words formed: " + actualWords);
        assertEquals(expectedWords, actualWords);
        
        int expectedScore = 9;

        // Assert the score
        assertEquals(expectedScore, score);

        // Print the score for verification
        System.out.println("Calculated score: " + score);
    }

    @Test
    void testComplexMultipleWordScoreIntegration() {
        // Use the real MoveValidatorService
        moveSubmitService = new MoveSubmitService(gameRepository, new MoveValidatorService(gameRepository));

        // Setup initial board with existing words
        String[][] currentBoard = createEmptyBoard();

        // Set up CAT vertically
        currentBoard[7][7] = "C";
        currentBoard[8][7] = "A";
        currentBoard[9][7] = "T";

        // Set up BAIT horizontally
        currentBoard[8][6] = "B";
        currentBoard[8][8] = "I";
        currentBoard[8][9] = "T";

        // Set up BARS horizontally
        currentBoard[10][4] = "B";
        currentBoard[10][5] = "A";
        currentBoard[10][6] = "R";
        currentBoard[10][7] = "S";

        testGame.setBoard(currentBoard);
        gameRepository.saveAndFlush(testGame);

        // Create new board with added tiles
        String[][] newBoard = createEmptyBoard();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                newBoard[i][j] = currentBoard[i][j];
            }
        }

        // Add new tiles for TATTOO horizontally and forming other perpendicular words
        newBoard[9][5] = "T";
        newBoard[9][6] = "A";
        newBoard[9][8] = "T";
        newBoard[9][9] = "O";
        newBoard[9][10] = "O";

        // Execute the submitMove method
        int score = moveSubmitService.submitMove(1L, newBoard);

        // Verify the words formed
        List<String> expectedWords = List.of("TATTOO", "TA", "BAR", "IT", "TO");
        List<String> actualWords = new MoveValidatorService(gameRepository).findWords(currentBoard, newBoard);

        System.out.println("Words formed: " + actualWords);
        assertEquals(expectedWords, actualWords);

        int expectedScore = 25;

        // Assert the score
        assertEquals(expectedScore, score);

        // Print the score for verification
        System.out.println("Calculated score: " + score);

    }
    
    @Test
    void testWordNotStartingNewLetterIntegration() {
        // Use the real MoveValidatorService
        moveSubmitService = new MoveSubmitService(gameRepository, new MoveValidatorService(gameRepository));

        // Setup initial board with existing words
        String[][] currentBoard = createEmptyBoard();

        // Set up CAT vertically
        currentBoard[7][7] = "C";
        currentBoard[8][7] = "A";
        currentBoard[9][7] = "T";

        testGame.setBoard(currentBoard);
        gameRepository.saveAndFlush(testGame);

        // Create new board with added tiles
        String[][] newBoard = createEmptyBoard();
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                newBoard[i][j] = currentBoard[i][j];
            }
        }

        // Add new tiles for TATTOO horizontally and forming other perpendicular words
        newBoard[10][7] = "S";

        // Execute the submitMove method
        int score = moveSubmitService.submitMove(1L, newBoard);

        // Verify the words formed
        List<String> expectedWords = List.of("CATS");
        List<String> actualWords = new MoveValidatorService(gameRepository).findWords(currentBoard, newBoard);

        System.out.println("Words formed: " + actualWords);
        assertEquals(expectedWords, actualWords);

        int expectedScore = 6;

        // Assert the score
        assertEquals(expectedScore, score);

        // Print the score for verification
        System.out.println("Calculated score: " + score);

    }
}