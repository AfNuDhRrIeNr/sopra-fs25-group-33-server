package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.BoardStatus;
import ch.uzh.ifi.hase.soprafs24.constant.LetterPoints;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class MoveSubmitService {

    private final GameRepository gameRepository;
    private final MoveValidatorService moveValidatorService;

    @Autowired
    public MoveSubmitService(GameRepository gameRepository, MoveValidatorService moveValidatorService) {
        this.gameRepository = gameRepository;
        this.moveValidatorService = moveValidatorService;
    }

    public int submitMove(Long gameId, String[][] newBoard) {

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        String[][] currentBoard = game.getBoard();

        // Find new tile positions
        List<int[]> newPositions = findNewTilePositions(currentBoard, newBoard);

        // Extract newly formed words
        List<String> formedWords = moveValidatorService.findWords(currentBoard, newBoard);

        // Calculate the total score
        int totalScore = calculateScore(currentBoard, newBoard, newPositions, formedWords);

        // Update the game board and save the new state
        game.setBoard(newBoard);
        gameRepository.save(game);

        return totalScore;
    }

    //calculates the total score of the move
    private int calculateScore(String[][] currentBoard, String[][] newBoard, List<int[]> newPositions, List<String> formedWords) {
        int totalScore = 0;

        //for every newly generated word
        for (String word : formedWords) {

            for (int[] pos : newPositions) {
                //make sure to check the right direction
                String foundWord = moveValidatorService.findWordAt(newBoard, pos[0], pos[1], true);
                boolean isHorizontal = true;

                //if the word is not found in the horizontal direction, check the vertical direction
                if (!word.equals(foundWord)) {
                    foundWord = moveValidatorService.findWordAt(newBoard, pos[0], pos[1], false);
                    isHorizontal = false;
                }

                if (word.equals(foundWord)) {
                    int[] startPos = findWordStartPosition(newBoard, pos[0], pos[1], isHorizontal);
                    totalScore += calculateWordScore(currentBoard, newBoard, startPos[0], startPos[1], isHorizontal, word);
                    break;
                }
            }
        }

        return totalScore;
    }

    //calculates the score per newly formed word
    private int calculateWordScore(String[][] oldBoard, String[][] newBoard, int startX, int startY, boolean isHorizontal, String word) {
        System.out.println("Calculating score for word: " + word);
        int wordScore = 0;
        int wordMultiplier = 1;

        int curX = startX; 
        int curY = startY;


        for (int i = 0; i < word.length(); i++) {
            char letter = word.charAt(i);
            int letterScore = LetterPoints.getPoints(letter);
            System.out.println("Letter: " + letter + ", Base score: " + letterScore);

            String multiplier = BoardStatus.getMultiplier(curX, curY);
            System.out.println("Multiplier at (" + curX + "," + curY + "): " + multiplier);

            if (BoardStatus.DOUBLE_LETTER.equals(multiplier)) {
                letterScore *= 2;
            } else if (BoardStatus.TRIPLE_LETTER.equals(multiplier)) {
                letterScore *= 3;
            } else if (BoardStatus.DOUBLE_WORD.equals(multiplier)) {
                wordMultiplier *= 2;
            } else if (BoardStatus.TRIPLE_WORD.equals(multiplier)) {
                wordMultiplier *= 3;
            }
        
            // Add the letter score to the word score
            wordScore += letterScore;

            if (isHorizontal) {
                curY++;
            } else {
                curX++;
            }
        }

        System.out.println("Word score before multiplier of " + wordMultiplier + ": " + wordScore);
        
        // Apply the word multiplier after calculating the letter scores
        wordScore *= wordMultiplier;
        System.out.println("Final word score: " + wordScore);

        return wordScore;
    }

    //helper function to get the positions of the newly placed tiles
    protected List<int[]> findNewTilePositions(String[][] oldBoard, String[][] newBoard) {
        List<int[]> newPositions = new ArrayList<>();

        for (int i = 0; i < oldBoard.length; i++) {
            for (int j = 0; j < oldBoard[0].length; j++) {
                if (oldBoard[i][j].equals("") && !newBoard[i][j].equals("")) {
                    newPositions.add(new int[]{i, j});
                }
            }
        }

        return newPositions;
    }

    //helper function to find the start position of a word
    private int[] findWordStartPosition(String[][] board, int x, int y, boolean horizontal) {
        int startX = x;
        int startY = y;

        if (horizontal) {
            while (startY > 0 && !board[startX][startY - 1].equals("")) {
                startY--;
            }
        } else {
            while (startX > 0 && !board[startX - 1][startY].equals("")) {
                startX--;
            }
        }

        return new int[]{startX, startY};
    }
}