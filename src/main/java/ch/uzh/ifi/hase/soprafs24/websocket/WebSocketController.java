package ch.uzh.ifi.hase.soprafs24.websocket;

import ch.uzh.ifi.hase.soprafs24.constant.MessageStatus;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MessageGameStateMessageDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs24.service.MoveValidatorService;
import ch.uzh.ifi.hase.soprafs24.service.MoveSubmitService;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import java.util.List;
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
                
                // 4. Return the updated game state to all players
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
        }

        // Default response for other cases
        return new MessageGameStateMessageDTO(
                Long.valueOf(gameId),
                MessageStatus.SUCCESS,
                "GameState successfully received",
                gameState
        );
    }

    // ------------------ Moves ---------------------------------------------
}
