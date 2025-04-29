package ch.uzh.ifi.hase.soprafs24.websocket;

import ch.uzh.ifi.hase.soprafs24.constant.MessageStatus;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MessageGameStateMessageDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs24.service.MoveValidatorService;
import ch.uzh.ifi.hase.soprafs24.service.MoveSubmitService;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import java.util.List;
import java.util.Random;
import java.util.Arrays;
import java.util.ArrayList;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.slf4j.Logger;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.GameNotFoundException;


@Controller
public class WebSocketController {

    @Autowired
    private MoveValidatorService moveValidatorService;

    @Autowired
    private MoveSubmitService moveSubmitService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    SimpMessagingTemplate simpleMessagingTemplate;

    @Autowired
    private GameService gameService;

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);

    // ------------------ Game State ---------------------------------------
    @MessageMapping("/game_states/{gameId}")
    @SendTo("/topic/game_states/{gameId}")
    public MessageGameStateMessageDTO handleGameStates(@DestinationVariable String gameId, GameStateDTO gameState) {
        logger.debug("[LOG] Game endpoint reached with gameId: '{}' and gameState entity: '{}'",gameId, gameState.toString());
        // Verify DTO
        try {
            gameState.isValid();
        } catch (IllegalArgumentException exception) {
            return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    exception.getMessage(),
                    null
            );
        }
        if(gameState.getId() != Long.valueOf(gameId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"The game id of the object and destination are not equal!");
        }

        // Check if it's a validation request
        if (gameState.getAction().equals("VALIDATE")) {
            try {
                // Validate the move
                List<String> formedWords = moveValidatorService.validateMoveAndExtractWords(
                        Long.valueOf(gameId), 
                        gameState.getBoard()
                );

                // Send validation success response ONLY to the requesting user
                simpleMessagingTemplate.convertAndSend(
                        "/topic/game_states/users/" + gameState.getPlayerId(), 
                        new MessageGameStateMessageDTO(
                            Long.valueOf(gameId),
                            MessageStatus.VALIDATION_SUCCESS,
                            "Move validation successful",
                            gameState
                        )
                );
                
                logger.debug("[LOG] Move validation successful for gameId: '{}', formed words: '{}'", gameId, formedWords);
                
                // Return null since we've already sent the message
                return null;
            } 
            catch (ResponseStatusException e) {
                // Extract invalid words from the error message
                List<String> invalidWords = extractInvalidWordsFromError(e.getMessage());
                
                // Set the invalid words in the gameState
                gameState.setRequestedWords(invalidWords);
                
                // Send validation error ONLY to the requesting user
                simpleMessagingTemplate.convertAndSend(
                        "/topic/game_states/users/" + gameState.getPlayerId(), 
                        new MessageGameStateMessageDTO(
                            Long.valueOf(gameId),
                            MessageStatus.VALIDATION_ERROR,
                            e.getMessage() + " (You can ask your friend if these words are valid)",
                            gameState
                        )
                );
                
                logger.debug("[LOG] Move validation failed for gameId: '{}', reason: '{}'", gameId, e.getReason());
                
                // Return null since we've already sent the message
                return null;
            }
        }
        else if (gameState.getAction().equals("SUBMIT")) {
            try {
                // 1. Calculate score using MoveSubmitService
                int score = moveSubmitService.submitMove(Long.valueOf(gameId), gameState.getBoard());
                
                // 2. Update the player's score
                Game game = gameRepository.findById(Long.valueOf(gameId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
                
                game.getPlayerScores();
                game.addScore(gameState.getPlayerId(), score);
                gameRepository.save(game);
                
                // 3. Add the updated scores to the response
                gameState.setPlayerScores(game.getPlayerScores());
                
                logger.info("Player {} scored {} points in game {}", gameState.getPlayerId(), score, gameId);
                // 4. draw tiles to fill hand
                int tilesToDraw = (int) Arrays.stream(gameState.getUserTiles())
                    .filter(tile -> tile.isEmpty())
                    .count();

                List<Character> newTiles = new Random().ints(tilesToDraw, 'A', 'Z' + 1)
                .mapToObj(c -> (char) c)
                .toList();

                // List<Character> newTiles = gameService.drawLetters(game, tilesToDraw);




                logger.info("TilesToDraw passed");
                // 5. Update the game state with the new tiles
                gameState.setUserTiles(newTiles.stream()
                    .map(String::valueOf) // Convert each Character to a String
                    .toArray(String[]::new));
                logger.info("UserTiles passed");
                // 6. Send the updated game state to the player
                simpleMessagingTemplate.convertAndSend(
                        "/topic/game_states/users/" + gameState.getPlayerId(), 
                        new MessageGameStateMessageDTO(
                            Long.valueOf(gameId),
                            MessageStatus.SUCCESS,
                            "Move submitted, scored " + score + " points",
                            gameState
                        )
                );
                logger.info("Personal message sent.");
                // 5. Return the updated game state to all players
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.SUCCESS,
                    "Move submitted, scored " + score + " points",
                    gameState
                );
            } 
            catch (ResponseStatusException e) {
                logger.error("Error processing move submission: {}", e.getReason());
                                     
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Error submitting move: " + e.getMessage(),
                    gameState
                );
            } catch (Exception e) {
                logger.error("Unexpected error: {}", e.getMessage());
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Unexpected error: " + e.getMessage(),
                    gameState
                );
            }
            
        }  else if (gameState.getAction().equals("SKIP") || gameState.getAction().equals("EXCHANGE")) {
            try {

                logger.info("Switching Turn for game: '{}'", gameId);
                // Skip the turn
                boolean isHostTurn = gameService.skipTurn(Long.valueOf(gameId), gameState.getPlayerId());
                gameState.setUserTiles(new String[7]);
                // Send skip success response to all players
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.SUCCESS,
                    gameState.getAction().equals("SKIP") ? "Move skipped" : "Tiles exchanged",
                    gameState
                );
            } catch (GameNotFoundException e) {
                logger.error("Game not found: {}", e.getMessage());
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Game not found: " + e.getMessage(),
                    gameState
                );
            } catch (ResponseStatusException e) {
                logger.error("Error processing turn skip: {}", e.getReason());
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Error skipping turn: " + e.getMessage(),
                    gameState
                );
            } catch (Exception e) {
                logger.info("Unexpected error: {}", e.getMessage());
                return null;
            }
        }
        else if (gameState.getAction().equals("REQUEST_VALIDATION")) {
            try {
                // Store requesting player's ID
                gameState.setRequestingPlayerId(gameState.getPlayerId());
                
                // Send to all players so opponent can respond
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.VALIDATION_REQUEST,
                    "Player " + gameState.getPlayerId() + " is asking if these words are valid: " + 
                        String.join(", ", gameState.getRequestedWords()),
                    gameState
                );
            } catch (Exception e) {
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Error requesting validation: " + e.getMessage(),
                    gameState
                );
            }
        }
        else if (gameState.getAction().equals("ACCEPT_WORD")) {
            try {
                // Calculate score and update game
                int score = moveSubmitService.submitMove(Long.valueOf(gameId), gameState.getBoard());
                
                Game game = gameRepository.findById(Long.valueOf(gameId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
                
                // Update requesting player's score
                game.addScore(gameState.getRequestingPlayerId(), score);
                gameRepository.save(game);
                
                // Add updated scores to response
                gameState.setPlayerScores(game.getPlayerScores());
                
                // Send response to all players
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.VALIDATION_ACCEPTED,
                    "Words accepted: " + String.join(", ", gameState.getRequestedWords()) + 
                        ", scored " + score + " points",
                    gameState
                );
            } catch (Exception e) {
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Error accepting words: " + e.getMessage(),
                    gameState
                );
            }
        }
        else if (gameState.getAction().equals("REJECT_WORD")) {
            // Simply notify that the words were rejected
            return new MessageGameStateMessageDTO(
                Long.valueOf(gameId),
                MessageStatus.VALIDATION_REJECTED,
                "Words rejected: " + String.join(", ", gameState.getRequestedWords()),
                gameState
            );
        }
        else {
            return null;
        }
    };
    

    // ------------------ Moves ---------------------------------------------


    private static char getRandomLetter() {
        Random random = new Random();
        return (char) ('A' + random.nextInt(26));
    }

    // Add this helper method to extract invalid words from error message
    private List<String> extractInvalidWordsFromError(String errorMessage) {
        List<String> words = new ArrayList<>();
        
        if (errorMessage != null && errorMessage.contains("not in the dictionary")) {
            int startSearch = 0;
            while (true) {
                int start = errorMessage.indexOf("'", startSearch);
                if (start == -1) break;
                
                int end = errorMessage.indexOf("'", start + 1);
                if (end == -1) break;
                
                words.add(errorMessage.substring(start + 1, end));
                startSearch = end + 1;
            }
        }
        
        return words;
    }
}
