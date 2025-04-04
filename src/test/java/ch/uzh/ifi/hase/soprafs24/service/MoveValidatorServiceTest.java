package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MoveValidatorServiceTest {

    @Mock
    private GameRepository gameRepository;
    
    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private MoveValidatorService moveValidatorService;

    private Game testGame;
    private char[][] boardWithH;
    private char[][] boardWithHAT;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test game
        testGame = new Game();
        testGame.setId(1L);
        
        // Create test boards to help with redundancy
        
        boardWithH = new char[15][15];
        boardWithH[7][7] = 'H';
        
        boardWithHAT = new char[15][15];
        boardWithHAT[7][7] = 'H';
        boardWithHAT[7][8] = 'A';
        boardWithHAT[7][9] = 'T';
        
        // Mocked RestTemplate
        ReflectionTestUtils.setField(moveValidatorService, "restTemplate", restTemplate);
        
        // Mock valid dictionary responses to test the isValidWord method in the service
        mockDictionaryForWord("hat", true);
        mockDictionaryForWord("hello", true);
        mockDictionaryForWord("world", true);
    }
    
    private void mockDictionaryForWord(String word, boolean isValid) {
        Object response = isValid ? 
            Collections.singletonList(new HashMap<>()) : // Valid word returns a list
            new HashMap<>(); // Invalid word returns a map (in the external API)
        when(restTemplate.getForObject(contains(word.toLowerCase()), eq(Object.class)))
            .thenReturn(response);
    }
    

    // Tests for dictionary (word) validation with external API
    
    @Test
    void isValidWord_validWord_returnsTrue() {
        // Given
        // Dictionary response for "hello" is already mocked in setUp()
        
        // When
        boolean result = ReflectionTestUtils.invokeMethod(moveValidatorService, "isValidWord", "hello");
        
        // Then
        assertTrue(result);
        verify(restTemplate).getForObject(contains("hello"), eq(Object.class));
    }
    
    @Test
    void isValidWord_invalidWord_returnsFalse() {
        // Given
        String invalidWord = "xyzabc";
        mockDictionaryForWord(invalidWord, false);
        
        // When
        boolean result = ReflectionTestUtils.invokeMethod(moveValidatorService, "isValidWord", invalidWord);
        
        // Then
        assertFalse(result);
    }
    

    // Tests for word finding
    
    @Test
    void findWords_horizontalWord_returnsCorrectWords() {
        // Given
        char[][] oldBoard = new char[15][15];
        oldBoard[7][7] = 'H';
        
        char[][] newBoard = new char[15][15];
        newBoard[7][7] = 'H';
        newBoard[7][8] = 'A';
        newBoard[7][9] = 'T';
        
        // When
        List<String> words = ReflectionTestUtils.invokeMethod(moveValidatorService, "findWords", oldBoard, newBoard);
        
        // Then
        assertEquals(1, words.size());
        assertEquals("HAT", words.get(0));
    }
    

    // Tests for validatePlacement
    
    @Test
    void validatePlacement_firstMoveNotAtCenter_throwsException() {
        // Given
        char[][] emptyBoard = new char[15][15];
        List<int[]> positionsNotAtCenter = Collections.singletonList(new int[]{0, 0});
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            ReflectionTestUtils.invokeMethod(moveValidatorService, "validatePlacement", 
                emptyBoard, positionsNotAtCenter, new char[15][15]));
    }
    
    @Test
    void validatePlacement_emptyBoardMoveNotCoveringCenter_throwsException() {
        // Given
        char[][] emptyBoard = new char[15][15];
        
        char[][] newBoard = new char[15][15];
        newBoard[5][5] = 'H';
        newBoard[5][6] = 'E';
        newBoard[5][7] = 'L';
        newBoard[5][8] = 'L';
        newBoard[5][9] = 'O';
        
        List<int[]> positions = Arrays.asList(
            new int[]{5, 5},
            new int[]{5, 6},
            new int[]{5, 7},
            new int[]{5, 8},
            new int[]{5, 9}
        );
        
        // When/Then
        assertThrows(IllegalArgumentException.class, () ->
            ReflectionTestUtils.invokeMethod(moveValidatorService, "validatePlacement", 
                emptyBoard, positions, newBoard));
    }
    
    @Test
    void validatePlacement_emptyBoardWithHorizontalWordThroughCenter_noException() {
        // Given
        char[][] emptyBoard = new char[15][15];
        
        char[][] newBoard = new char[15][15];
        newBoard[7][5] = 'H';
        newBoard[7][6] = 'E';
        newBoard[7][7] = 'L';
        newBoard[7][8] = 'L';
        newBoard[7][9] = 'O';
        
        List<int[]> positions = Arrays.asList(
            new int[]{7, 5},
            new int[]{7, 6},
            new int[]{7, 7},
            new int[]{7, 8},
            new int[]{7, 9}
        );
        
        // When/Then
        assertDoesNotThrow(() ->
            ReflectionTestUtils.invokeMethod(moveValidatorService, "validatePlacement", 
                emptyBoard, positions, newBoard));
    }
    
    // Tests for directional checks
    
    @Test
    void isHorizontal_allSameRow_returnsTrue() {
        // Given
        List<int[]> horizontalPositions = Arrays.asList(
            new int[]{5, 5},
            new int[]{5, 6},
            new int[]{5, 7}
        );
        
        // When
        boolean result = ReflectionTestUtils.invokeMethod(moveValidatorService, "isHorizontal", horizontalPositions);
        
        // Then
        assertTrue(result);
    }
    
    @Test
    void isVertical_allSameColumn_returnsTrue() {
        // Given
        List<int[]> verticalPositions = Arrays.asList(
            new int[]{5, 5},
            new int[]{6, 5},
            new int[]{7, 5}
        );
        
        // When
        boolean result = ReflectionTestUtils.invokeMethod(moveValidatorService, "isVertical", verticalPositions);
        
        // Then
        assertTrue(result);
    }
    

    // Tests for findWordAt
    
    @Test
    void findWordAt_horizontalWord_returnsFullWord() {
        // Given
        char[][] board = new char[15][15];
        board[7][7] = 'H';
        board[7][8] = 'E';
        board[7][9] = 'L';
        board[7][10] = 'L';
        board[7][11] = 'O';
        
        // When
        String word = ReflectionTestUtils.invokeMethod(moveValidatorService, "findWordAt", 
            board, 7, 9, true);
        
        // Then
        assertEquals("HELLO", word);
    }
    
    @Test
    void findWordAt_verticalWord_returnsFullWord() {
        // Given
        char[][] board = new char[15][15];
        board[5][7] = 'W';
        board[6][7] = 'O';
        board[7][7] = 'R';
        board[8][7] = 'L';
        board[9][7] = 'D';
        
        // When
        String word = ReflectionTestUtils.invokeMethod(moveValidatorService, "findWordAt", 
            board, 7, 7, false);
        
        // Then
        assertEquals("WORLD", word);
    }
    
    @Test
    void isHorizontal_differentRows_returnsFalse() {
        // Given
        List<int[]> nonHorizontalPositions = Arrays.asList(
            new int[]{5, 5},
            new int[]{6, 6},
            new int[]{7, 7}
        );
        
        // When
        boolean result = ReflectionTestUtils.invokeMethod(moveValidatorService, "isHorizontal", nonHorizontalPositions);
        
        // Then
        assertFalse(result);
    }
    
    @Test
    void isVertical_differentColumns_returnsFalse() {
        // Given
        List<int[]> nonVerticalPositions = Arrays.asList(
            new int[]{5, 5},
            new int[]{6, 6},
            new int[]{7, 7}
        );
        
        // When
        boolean result = ReflectionTestUtils.invokeMethod(moveValidatorService, "isVertical", nonVerticalPositions);
        
        // Then
        assertFalse(result);
    }
}
