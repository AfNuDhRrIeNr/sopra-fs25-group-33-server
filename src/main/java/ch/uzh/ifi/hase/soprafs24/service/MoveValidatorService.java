package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * Move Validator Service
 * This class is responsible for validating Scrabble move placements and extracting words formed
 */
@Service
@Transactional
public class MoveValidatorService {

    private final Logger log = LoggerFactory.getLogger(MoveValidatorService.class);

    private final GameRepository gameRepository;

    @Autowired
    public MoveValidatorService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    /**
     * Validates a move placement and extracts words formed
     * @param gameId ID of the game
     * @param newBoard Proposed new board state
     * @return List of words formed by the new move
     */
    public List<String> validateMoveAndExtractWords(Long gameId, char[][] newBoard) {
        // Get the game from repository
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        // Get current board state
        char[][] currentBoard = game.getBoard();
        
        
        try {
            // Validate move and get formed words
            List<String> formedWords = findWords(currentBoard, newBoard);
            
            // If no valid words were formed, throw an exception
            if (formedWords.isEmpty()) {
                throw new IllegalArgumentException("No valid words formed");
            }
            
            return formedWords;
        } 
        catch (IllegalArgumentException e) {
            log.error("Move validation failed: {}", e.getMessage());
            // Convert validation exceptions to HTTP exceptions
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // "Main" method to find words formed by new tiles
    private List<String> findWords(char[][] oldBoard, char[][] newBoard) {
        // Find new tile positions
        List<int[]> newPositions = new ArrayList<>();
        
        // Compare boards to find new tiles positions
        for (int i = 0; i < oldBoard.length; i++) {
            for (int j = 0; j < oldBoard[0].length; j++) {
                if (oldBoard[i][j] == '\0' && newBoard[i][j] != '\0') {
                    newPositions.add(new int[]{i, j});
                }
            }
        }
        
        // If no new tiles, return empty list
        if (newPositions.isEmpty()) {
            throw new IllegalArgumentException("No new tiles placed");
        }
        
        // Validate placement (straight line and connected tiles)
        validatePlacement(oldBoard, newPositions, newBoard);
        
        // Find all words
        List<String> words = new ArrayList<>();
        boolean isHorizontal = isHorizontal(newPositions);
        
        // Find the main word
        String mainWord = findWordAt(newBoard, newPositions.get(0)[0], newPositions.get(0)[1], isHorizontal);
        if (mainWord.length() > 1) {
            words.add(mainWord); // Add the main word first
        }
        
        // Find perpendicular words
        for (int[] pos : newPositions) {
            String perpWord = findWordAt(newBoard, pos[0], pos[1], !isHorizontal);
            if (perpWord.length() > 1) {
                words.add(perpWord);
            }
        }
        
        return words;
    }
    
    private boolean isHorizontal(List<int[]> positions) {
        return positions.stream().allMatch(pos -> pos[0] == positions.get(0)[0]);
    }

    private boolean isVertical(List<int[]> positions) {
        return positions.stream().allMatch(pos -> pos[1] == positions.get(0)[1]);
    }
    
    private void validatePlacement(char[][] board, List<int[]> positions, char[][] newBoard) {
        boolean isHorz = isHorizontal(positions);
        boolean isVert = isVertical(positions);
        
        if (!isHorz && !isVert) {
            throw new IllegalArgumentException("New tiles must be placed in a straight line");
        }
        
        // Check if this is the first move (board is empty)
        boolean boardIsEmpty = true;
        for (int i = 0; i < board.length && boardIsEmpty; i++) {
            for (int j = 0; j < board[0].length; j++) {
                if (board[i][j] != '\0') {
                    boardIsEmpty = false;
                    break;
                }
            }
        }
        
        if (boardIsEmpty) {
            // For the first move, one tile must cover the center square (7,7)
            int centerX = 7;
            int centerY = 7;
            boolean coversCenterSquare = positions.stream()
                .anyMatch(pos -> pos[0] == centerX && pos[1] == centerY);
                
            if (!coversCenterSquare) {
                throw new IllegalArgumentException("First word must cover the center square");
            }
            
            return; // Skip the connectivity check for the first move
        }

        // check if every new tile is connected to another tile (already on the board or new)
        boolean allConnected = true;
        for (int[] pos : positions) {
            int x = pos[0], y = pos[1];
            boolean tileConnected = 
                (x > 0 && newBoard[x-1][y] != '\0') || 
                (x < newBoard.length-1 && newBoard[x+1][y] != '\0') ||
                (y > 0 && newBoard[x][y-1] != '\0') ||
                (y < newBoard[0].length-1 && newBoard[x][y+1] != '\0');
                
            if (!tileConnected) {
                allConnected = false;
                break;
            }
        }
        
        if (!allConnected) {
            throw new IllegalArgumentException("All new tiles must connect to other tiles");
        }

        // check if at least one new tile connects to existing tiles
        boolean anyConnectedToExisting = false;
        for (int[] pos : positions) {
            int x = pos[0], y = pos[1];
            if ((x > 0 && board[x-1][y] != '\0') || 
                (x < board.length-1 && board[x+1][y] != '\0') ||
                (y > 0 && board[x][y-1] != '\0') ||
                (y < board[0].length-1 && board[x][y+1] != '\0')) {
                anyConnectedToExisting = true;
                break; // Once we find one connection, we can stop checking
            }
        }

        if (!anyConnectedToExisting) {
            throw new IllegalArgumentException("New tiles must connect to existing tiles");
        }
    }
    
    private String findWordAt(char[][] board, int x, int y, boolean horizontal) {
        StringBuilder word = new StringBuilder();
        
        // Find start of word
        int startX = x, startY = y;
        if (horizontal) {
            while (startY > 0 && board[startX][startY-1] != '\0') startY--;
        } else {
            while (startX > 0 && board[startX-1][startY] != '\0') startX--;
        }
        
        // Build word
        int curX = startX, curY = startY;
        if (horizontal) {
            while (curY < board[0].length && board[curX][curY] != '\0') {
                word.append(board[curX][curY]);
                curY++;
            }
        } else {
            while (curX < board.length && board[curX][curY] != '\0') {
                word.append(board[curX][curY]);
                curX++;
            }
        }
        
        return word.toString();
    }

}
