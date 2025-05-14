package ch.uzh.ifi.hase.soprafs24.websocket;

import ch.uzh.ifi.hase.soprafs24.constant.MessageStatus;
import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MessageGameStateMessageDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs24.service.MoveValidatorService;
import ch.uzh.ifi.hase.soprafs24.service.MoveSubmitService;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import java.util.List;
import java.util.Random;
import java.util.Arrays;
import java.time.LocalDateTime;
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
                // Send validation error ONLY to the requesting user
                simpleMessagingTemplate.convertAndSend(
                        "/topic/game_states/users/" + gameState.getPlayerId(), 
                        new MessageGameStateMessageDTO(
                            Long.valueOf(gameId),
                            MessageStatus.VALIDATION_ERROR,
                            e.getMessage(),
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
        else if (gameState.getAction().equals("GAME_END") || gameState.getAction().equals("SURRENDER")) {
            try {
                logger.info("Game {} is ending. Triggered by player {}", gameId, gameState.getPlayerId());
        
                Game game = gameRepository.findById(Long.valueOf(gameId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
                
                List<User> users = game.getUsers();

                for (User user : users) {
                    Integer playerScore = game.getPlayerScores().get(user.getId());
                    if (playerScore != null && user.getHighScore() < playerScore) {
                        user.setHighScore(playerScore);
                    }
                    user.setInGame(false);
                }
                game.setGameStatus(GameStatus.TERMINATED);
                gameRepository.save(game);
        
                logger.info("Game {} has been successfully terminated.", gameId);

                if (gameState.getAction().equals("SURRENDER")) {
                    gameState.setSurrenderedPlayerId(gameState.getPlayerId());
                    simpleMessagingTemplate.convertAndSend(
                        "/topic/game_states/" + gameId,
                        new MessageGameStateMessageDTO(
                            Long.valueOf(gameId),
                            MessageStatus.SUCCESS,
                            "Player " + gameState.getPlayerId() + " has surrendered.",
                            gameState
                        )
                    );
                } else {
                    simpleMessagingTemplate.convertAndSend(
                        "/topic/game_states/" + gameId,
                        new MessageGameStateMessageDTO(
                            Long.valueOf(gameId),
                            MessageStatus.SUCCESS,
                            "The game has ended.",
                            gameState
                        )
                    );
                }

                return null;
            } catch (ResponseStatusException e) {
                logger.error("Error processing game end action: {}", e.getReason());
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Error ending game: " + e.getMessage(),
                    gameState
                );
            } catch (Exception e) {
                logger.error("Unexpected error during game end: {}", e.getMessage());
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Unexpected error: " + e.getMessage(),
                    gameState
                );
            }
        }else if (gameState.getAction().equals("VOTE") || gameState.getAction().equals("NO_VOTE")) {
            try {
                logger.info("Inside Vote handler for game: '{}'", gameId);

                Game game = gameRepository.findById(Long.valueOf(gameId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

                List<User> users = game.getUsers();

                Long senderId = gameState.getPlayerId();
                User otherUser = users.stream()
                    .filter(user -> !user.getId().equals(senderId)) // Exclude the sender
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Other user not found in the game"));

                if (gameState.getAction().equals("VOTE")) {
                    simpleMessagingTemplate.convertAndSend(
                    "/topic/game_states/users/" + otherUser.getId(),
                    new MessageGameStateMessageDTO(
                        Long.valueOf(gameId),
                        MessageStatus.SUCCESS,
                        "Player " + senderId + " has started a game end vote.",
                        gameState
                    )
                );
                } else {
                    simpleMessagingTemplate.convertAndSend(
                    "/topic/game_states/users/" + otherUser.getId(),
                    new MessageGameStateMessageDTO(
                        Long.valueOf(gameId),
                        MessageStatus.SUCCESS,
                        "Player " + senderId + " declined.",
                        gameState
                    )
                );
                }


                return null;
            } catch (ResponseStatusException e) {
                logger.error("Error processing game start action: {}", e.getReason());
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Error starting game: " + e.getMessage(),
                    gameState
                );
            } catch (Exception e) {
                logger.error("Unexpected error during game start: {}", e.getMessage());
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Unexpected error: " + e.getMessage(),
                    gameState
                );
            }
        } else if (gameState.getAction().equals("TIMER")) {
            try {
                
                Game game = gameRepository.findById(Long.valueOf(gameId))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

                LocalDateTime now = LocalDateTime.now();
                long elapsedSeconds = java.time.Duration.between(game.getStartTime(), now).toSeconds();
                long remainingSeconds = 45 * 60 - elapsedSeconds;
              
                gameState.setRemainingTime(remainingSeconds);
                simpleMessagingTemplate.convertAndSend(
                    "/topic/game_states/" + gameId,
                    new MessageGameStateMessageDTO(
                        Long.valueOf(gameId),
                        MessageStatus.SUCCESS,
                        "Timer synchronized. Remaining time: " + remainingSeconds + " seconds.",
                        gameState
                    )
                );
        
                return null;
            } catch (ResponseStatusException e) {
                logger.error("Error during timer synchronization: {}", e.getReason());
                return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Error during timer synchronization: " + e.getMessage(),
                    gameState
                );
            }
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
}
