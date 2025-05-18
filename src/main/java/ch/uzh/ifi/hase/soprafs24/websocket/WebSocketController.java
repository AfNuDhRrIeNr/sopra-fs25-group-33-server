package ch.uzh.ifi.hase.soprafs24.websocket;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.constant.MessageStatus;
import ch.uzh.ifi.hase.soprafs24.constant.errors.GameNotFoundException;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MessageGameStateMessageDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.MoveSubmitService;
import ch.uzh.ifi.hase.soprafs24.service.MoveValidatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Controller
public class WebSocketController {

    @Autowired
    private MoveValidatorService moveValidatorService;

    @Autowired
    private MoveSubmitService moveSubmitService;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private UserRepository userRepository;

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
            handleValidate(gameId, gameState);
        }

        else if (gameState.getAction().equals("SUBMIT")) {
            return handleSubmit(gameId, gameState);

        }  else if (gameState.getAction().equals("SKIP")) {
            return handleSkip(gameId, gameState);
        } else if (gameState.getAction().equals("EXCHANGE")) {
            return handleExchange(gameId, gameState);
        }
        else if (gameState.getAction().equals("FETCH_GAME_STATE")) {
            return handleFetchGameState(gameId, gameState);
        }
        else if (gameState.getAction().equals("GAME_END") || gameState.getAction().equals("SURRENDER")) {
            try {
                logger.info("Game {} is ending. Triggered by player {}", gameId, gameState.getPlayerId());

                Game game = gameRepository.findByIdWithUsers(Long.valueOf(gameId))
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

                List<User> users = game.getUsers();

                for (User user : users) {
                    Integer playerScore = game.getPlayerScores().get(user.getId());
                    if (playerScore != null && user.getHighScore() < playerScore) {
                        user.setHighScore(playerScore);
                    }
                    user.setInGame(false);
                    userRepository.saveAndFlush(user);
                }
        
                logger.info("Game {} has been successfully terminated.", gameId);

                if (gameState.getAction().equals("SURRENDER")) {
                    gameState.setSurrenderedPlayerId(gameState.getPlayerId());
                    game.setGameStatus(GameStatus.TERMINATED);
                    game.setSurrenderId(gameState.getPlayerId());
                    gameRepository.saveAndFlush(game); 
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
                    game.setGameStatus(GameStatus.TERMINATED);
                    gameRepository.saveAndFlush(game);
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

                Game game = gameRepository.findByIdWithUsers(Long.valueOf(gameId))
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
            logger.error("Unknown action: {}", gameState.getAction());
            return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Unknown action: " + gameState.getAction(),
                    null
            );
        }
        return null;
    }


    private MessageGameStateMessageDTO handleSubmit(String gameId, GameStateDTO gameState) {
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
            String[] newTiles = gameService.assignNewLetters(game, gameState.getPlayerId(), gameState.getUserTiles());
            logger.info("TilesToDraw passed");
            // 5. Update the game state with the new tiles
            gameState.setUserTiles(newTiles);
            logger.info("UserTiles passed");
            //6. Update the game state with user turn
            User userAtTurn = gameService.changeUserTurn(game);
            gameState.setPlayerId(userAtTurn.getId());
            // 7. Send the updated game state to the player
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
            // 8. Return the updated game state to all players
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
        }
        catch (Exception e) {
            logger.error("Unexpected error: {}", e.getMessage());
            return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Unexpected error: " + e.getMessage(),
                    gameState

            );
        }
    }

    private MessageGameStateMessageDTO handleFetchGameState(String gameId, GameStateDTO gameState) {
        Optional<Game> gameOptional = gameService.getGameById(Long.valueOf(gameId));
        if(gameOptional.isEmpty()) {
            logger.error("Error while fetching game state. Game was not found.");
            MessageGameStateMessageDTO message = new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Error while fetching game state. Game was not found.",
                    gameState
            );
            simpleMessagingTemplate.convertAndSend(
                    "/topic/game_states/users/" + gameState.getPlayerId(),
                    message
            );
            return message;
        }

        Long senderId = gameState.getPlayerId();
        Game game = gameRepository.findByIdWithUsers(Long.valueOf(gameId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        if(!game.getUsers().stream().filter(user -> user.getId().equals(senderId)).findFirst().isPresent()) {
            logger.error("Error while fetching game state. User is not part of the game.");
            return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Error while fetching game state. User is not part of the game.",
                    gameState
            );
        }

        if(game.getPlayerTiles(senderId).length == 0) {
            logger.info("User has no tiles. Drawing new tiles.");
            String[] letters = {};
            gameService.assignNewLetters(game, senderId, letters);
        }

        gameState.setBoard(game.getBoard());
        gameState.setUserTiles(game.getPlayerTiles(gameState.getPlayerId()));
        gameState.setPlayerScores(game.getPlayerScores());
        gameState.setAction("FETCH_GAME_STATE");
        gameState.setPlayerId(game.isHostTurn() ? game.getHost().getId() : game.getUsers().get(1).getId());
        MessageGameStateMessageDTO message = new MessageGameStateMessageDTO(
                Long.valueOf(gameId),
                MessageStatus.SUCCESS,
                "Game state fetched successfully",
                gameState
        );
        simpleMessagingTemplate.convertAndSend(
                "/topic/game_states/users/" + senderId,
                message

        );
        return message;
    }

    private MessageGameStateMessageDTO handleSkip(String gameId, GameStateDTO gameState) {
        try {

            logger.info("Switching Turn for game: '{}'", gameId);
            // Skip the turn
            boolean isHostTurn = gameService.skipTurn(Long.valueOf(gameId), gameState.getPlayerId());
            //gameState.setUserTiles(new String[7]);
            Optional<Game> optional = gameRepository.findByIdWithUsers(Long.valueOf(gameId));
            if(optional.isEmpty()) return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Game not found",
                    gameState
            );
            Game game = optional.get();
            gameState.setPlayerId(isHostTurn ? game.getHost().getId() : game.getUsers().get(1).getId());
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

    private MessageGameStateMessageDTO handleExchange(String gameId, GameStateDTO gameState) {
        try {

            logger.info("Exchanging tiles for game: '{}' and user '{}'", gameId, gameState.getPlayerId());
            // Skip the turn
            boolean isHostTurn = gameService.skipTurn(Long.valueOf(gameId), gameState.getPlayerId());
            Optional<Game> optional = gameRepository.findByIdWithUsers(Long.valueOf(gameId));
            if(optional.isEmpty()) return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    "Game not found",
                    gameState
            );
            Game game = optional.get();
            gameService.exchangeTiles(game, gameState.getUserTiles(), gameState.getPlayerId());
            gameState.setUserTiles(game.getPlayerTiles(gameState.getPlayerId()));
            gameState.setPlayerId(isHostTurn ? game.getHost().getId() : game.getUsers().get(1).getId());
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

        }
        return null;
    }


    private void handleValidate(String gameId, GameStateDTO gameState) {
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
        }
    }
}
