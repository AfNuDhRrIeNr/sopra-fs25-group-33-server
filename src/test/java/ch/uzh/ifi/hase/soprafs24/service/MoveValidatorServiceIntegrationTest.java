package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@WebAppConfiguration
@SpringBootTest
class MoveValidatorServiceIntegrationTest {

    @Autowired
    private MoveValidatorService moveValidatorService;

    @Autowired
    private GameRepository gameRepository;

    private Game testGame;

    @BeforeEach
    void setUp() {
        // Clear database
        gameRepository.deleteAll();
        
        // Create a test game
        testGame = new Game();
        testGame.initializeEmptyBoard();
        testGame = gameRepository.saveAndFlush(testGame);
    }

    @Test
    void validateMoveAndExtractWords_firstMove_success() {
        // First valid move with "HELLO" at center
        char[][] firstMove = new char[15][15];
        firstMove[7][7] = 'H';
        firstMove[7][8] = 'E';
        firstMove[7][9] = 'L';
        firstMove[7][10] = 'L';
        firstMove[7][11] = 'O';
        
        // Test
        List<String> words = moveValidatorService.validateMoveAndExtractWords(testGame.getId(), firstMove);
        
        // Verify
        assertEquals(1, words.size());
        assertEquals("HELLO", words.get(0));
    }

    @Test
    void validateMoveAndExtractWords_invalidFirstMove_throwsException() {
        // First move not crossing center
        char[][] invalidMove = new char[15][15];
        invalidMove[0][0] = 'X';
        
        // Test and assert
        assertThrows(ResponseStatusException.class, () -> 
            moveValidatorService.validateMoveAndExtractWords(testGame.getId(), invalidMove)
        );
    }

    @Test
    void validateMoveAndExtractWords_secondMove_success() {
        // Set up first move
        char[][] firstMove = new char[15][15];
        firstMove[7][7] = 'H';
        firstMove[7][8] = 'E';
        firstMove[7][9] = 'L';
        firstMove[7][10] = 'L';
        firstMove[7][11] = 'O';
        testGame.setBoard(firstMove);
        gameRepository.saveAndFlush(testGame);
        
        // Second move forming perpendicular word
        char[][] secondMove = new char[15][15];
        // Copy the existing word
        secondMove[7][7] = 'H';
        secondMove[7][8] = 'E';
        secondMove[7][9] = 'L';
        secondMove[7][10] = 'L';
        secondMove[7][11] = 'O';
        // Add 'P' to form "LP" with the 'L'
        secondMove[8][9] = 'P'; 
        
        // Test
        List<String> words = moveValidatorService.validateMoveAndExtractWords(testGame.getId(), secondMove);
        
        // Verify at least one word is found (should be "LP")
        assertFalse(words.isEmpty());
        // The perpendicular word should be "LP"
        assertTrue(words.contains("LP"));
    }

    @Test
    void validateMoveAndExtractWords_newTileMustConnectToExistingTiles_throwsException() {
        // Set up the board with existing tiles
        char[][] initialBoard = new char[15][15];
        initialBoard[7][7] = 'H';
        initialBoard[8][7] = 'A';
        initialBoard[9][7] = 'T';
        testGame.setBoard(initialBoard);
        gameRepository.saveAndFlush(testGame);

        // Attempt to place new tiles that don't connect to existing tiles
        char[][] newBoard = new char[15][15];
        for (int i = 0; i < initialBoard.length; i++) {
            System.arraycopy(initialBoard[i], 0, newBoard[i], 0, initialBoard[i].length);
        }
        newBoard[5][5] = 'P';
        newBoard[5][6] = 'T';

        // Test and assert
        assertThrows(ResponseStatusException.class, () ->
            moveValidatorService.validateMoveAndExtractWords(testGame.getId(), newBoard)
        );
    }

    @Test
    void validateMoveAndExtractWords_NewTilesMustConnectToEachOther_throwsException() {
        // Set up the board with existing tiles
        char[][] initialBoard = new char[15][15];
        initialBoard[7][7] = 'H';
        initialBoard[8][7] = 'A';
        initialBoard[9][7] = 'T';
        testGame.setBoard(initialBoard);
        gameRepository.saveAndFlush(testGame);

        // Attempt to place new tiles that don't connect to each other
        char[][] newBoard = new char[15][15];
        for (int i = 0; i < initialBoard.length; i++) {
            System.arraycopy(initialBoard[i], 0, newBoard[i], 0, initialBoard[i].length);
        }
        newBoard[7][9] = 'P';
        newBoard[9][9] = 'T';

        // Test and assert
        assertThrows(ResponseStatusException.class, () ->
            moveValidatorService.validateMoveAndExtractWords(testGame.getId(), newBoard)
        );
    }

    @Test
    void validateMoveAndExtractWords_tilesMustBePlacedInStraightLine_throwsException() {
        // Set up the board with existing tiles
        char[][] initialBoard = new char[15][15];
        initialBoard[7][7] = 'H';
        initialBoard[8][7] = 'A';
        initialBoard[9][7] = 'T';
        testGame.setBoard(initialBoard);
        gameRepository.saveAndFlush(testGame);

        // Attempt to place new tiles that are not in a straight line
        char[][] newBoard = new char[15][15];
        for (int i = 0; i < initialBoard.length; i++) {
            System.arraycopy(initialBoard[i], 0, newBoard[i], 0, initialBoard[i].length);
        }
        newBoard[7][9] = 'P';
        newBoard[8][8] = 'T';

        // Test and assert
        assertThrows(ResponseStatusException.class, () ->
            moveValidatorService.validateMoveAndExtractWords(testGame.getId(), newBoard)
        );
    }

    @Test
    void validateMoveAndExtractWords_firstWordMustCoverCenterSquare_throwsException() {
        // Initialize an empty board
        testGame.initializeEmptyBoard();
        gameRepository.saveAndFlush(testGame);

        // Attempt to place the first word without covering the center square
        char[][] newBoard = new char[15][15];
        newBoard[5][5] = 'A';
        newBoard[5][6] = 'B';
        newBoard[5][7] = 'C';

        // Test and assert
        assertThrows(ResponseStatusException.class, () ->
            moveValidatorService.validateMoveAndExtractWords(testGame.getId(), newBoard)
        );
    }

    @Test
    void validateMoveAndExtractWords_validMoveWithMultipleWords_success() {
        // Set up the board with existing tiles
        char[][] initialBoard = new char[15][15];
        initialBoard[7][7] = 'C';
        initialBoard[8][7] = 'A';
        initialBoard[9][7] = 'T';
        initialBoard[8][6] = 'B';
        initialBoard[8][8] = 'I';
        initialBoard[8][9] = 'T';
        initialBoard[10][4] = 'B';
        initialBoard[10][5] = 'A';
        initialBoard[10][6] = 'R';
        initialBoard[10][7] = 'S';
        testGame.setBoard(initialBoard);
        gameRepository.saveAndFlush(testGame);

        // Place new tiles to form multiple words
        char[][] newBoard = new char[15][15];
        for (int i = 0; i < initialBoard.length; i++) {
            System.arraycopy(initialBoard[i], 0, newBoard[i], 0, initialBoard[i].length);
        }
        newBoard[9][5] = 'T';
        newBoard[9][6] = 'A';
        newBoard[9][8] = 'T';
        newBoard[9][9] = 'O'; 
        newBoard[9][10] = 'O';

        // Test
        List<String> words = moveValidatorService.validateMoveAndExtractWords(testGame.getId(), newBoard);

        // Verify
        assertEquals(5, words.size());
        assertTrue(words.contains("TATTOO"));
        assertTrue(words.contains("BAR"));
        assertTrue(words.contains("TA"));
        assertTrue(words.contains("IT"));   
        assertTrue(words.contains("TO"));
    }
}
