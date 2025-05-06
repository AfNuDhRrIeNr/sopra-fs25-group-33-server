package ch.uzh.ifi.hase.soprafs24.websocket;

import ch.uzh.ifi.hase.soprafs24.constant.MessageStatus;
import ch.uzh.ifi.hase.soprafs24.constant.MoveType;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MessageGameStateMessageDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs24.service.MoveValidatorService;
import ch.uzh.ifi.hase.soprafs24.service.MoveSubmitService;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Arrays;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
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
        logger.info("Game endpoint reached with gameId: '{}' and gameState entity: '{}'",gameId, gameState.toString());
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
        else if (gameState.getAction().equals("FETCH_GAME_STATE")) {
            Optional<Game> gameOptional = gameService.getGameById(Long.valueOf(gameId));
            if(gameOptional.isEmpty()) {
                logger.error("Error while fetching game state. Game was not found.");
                return new MessageGameStateMessageDTO(
                        Long.valueOf(gameId),
                        MessageStatus.ERROR,
                        "Error while fetching game state. Game was not found.",
                        gameState
                );
            }
            Long senderId = gameState.getPlayerId();
            Game game = gameOptional.get();
            gameState.setBoard(game.getBoard());
            gameState.setUserTiles(game.getPlayerTiles(gameState.getPlayerId()));
            gameState.setPlayerScores(game.getPlayerScores());
            gameState.setAction("FETCH_GAME_STATE");
            gameState.setPlayerId(game.isHostTurn() ? game.getHost().getId() : game.getUsers().get(1).getId());
            simpleMessagingTemplate.convertAndSend(
                    "/topic/game_states/users/" + senderId,
                    new MessageGameStateMessageDTO(
                            Long.valueOf(gameId),
                            MessageStatus.SUCCESS,
                            "Game state fetched successfully",
                            gameState
                    )
            );
        }
        return null;
    }


    // ------------------ Moves ---------------------------------------------


    private static char getRandomLetter() {
        Random random = new Random();
        return (char) ('A' + random.nextInt(26));
    }
}
