package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MoveValidatorServiceTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private MoveValidatorService moveValidatorService;

    private Game testGame;
    private char[][] emptyBoard;
    private char[][] boardWithH;
    private char[][] boardWithHAT;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test game
        testGame = new Game();
        testGame.setId(1L);
        
        // Create test boards
        emptyBoard = new char[15][15];
        
        boardWithH = new char[15][15];
        boardWithH[7][7] = 'H';
        
        boardWithHAT = new char[15][15];
        boardWithHAT[7][7] = 'H';
        boardWithHAT[7][8] = 'A';
        boardWithHAT[7][9] = 'T';
    }

    @Test
    void validateMoveAndExtractWords_gameNotFound_throwsException() {
        // Mock repository behavior
        when(gameRepository.findById(anyLong())).thenReturn(Optional.empty());

        // Test and assert
        assertThrows(ResponseStatusException.class, () -> 
            moveValidatorService.validateMoveAndExtractWords(1L, emptyBoard)
        );
    }

    @Test
    void validateMoveAndExtractWords_firstMove_success() {
        // Setup
        testGame.setBoard(emptyBoard);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        
        // First move with H in center
        char[][] firstMove = new char[15][15];
        firstMove[7][7] = 'H';
        firstMove[7][8] = 'E';
        
        // Test
        List<String> words = moveValidatorService.validateMoveAndExtractWords(1L, firstMove);
        
        // Verify
        verify(gameRepository).findById(1L);
        assertEquals(1, words.size());
        assertEquals("HE", words.get(0));
    }

    @Test
    void validateMoveAndExtractWords_addToExistingWord_success() {
        // Setup
        testGame.setBoard(boardWithH);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        
        // Test
        List<String> words = moveValidatorService.validateMoveAndExtractWords(1L, boardWithHAT);
        
        // Verify
        verify(gameRepository).findById(1L);
        assertEquals(1, words.size());
        assertEquals("HAT", words.get(0));
    }

    @Test
    void validateMoveAndExtractWords_invalidPlacement_throwsException() {
        // Setup
        testGame.setBoard(emptyBoard);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        
        // First move not at center
        char[][] invalidFirstMove = new char[15][15];
        invalidFirstMove[0][0] = 'X';
        
        // Test and assert
        assertThrows(ResponseStatusException.class, () -> 
            moveValidatorService.validateMoveAndExtractWords(1L, invalidFirstMove)
        );
    }

    @Test
    void validateMoveAndExtractWords_formsPerpendicular_success() {
        // Setup
        testGame.setBoard(boardWithH);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));
        
        // Form perpendicular word "HE"
        char[][] perpMove = new char[15][15];
        perpMove[7][7] = 'H';
        perpMove[8][7] = 'E';
        
        // Test
        List<String> words = moveValidatorService.validateMoveAndExtractWords(1L, perpMove);
        
        // Verify
        verify(gameRepository).findById(1L);
        assertEquals(1, words.size());
        assertEquals("HE", words.get(0));
    }
}
