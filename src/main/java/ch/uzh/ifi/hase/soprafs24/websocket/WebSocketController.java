package ch.uzh.ifi.hase.soprafs24.websocket;

import ch.uzh.ifi.hase.soprafs24.constant.MessageStatus;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MessageGameStateMessageDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs24.service.MoveValidatorService;
import java.util.List;
import java.util.Random;

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
                            e.getReason(),
                            gameState
                        )
                );
                
                logger.debug("[LOG] Move validation failed for gameId: '{}', reason: '{}'", gameId, e.getReason());
                
                // Return null since we've already sent the message
                return null;
            }
        }
        else if (gameState.getAction().equals("SUBMIT")) {
            // For submit, just broadcast the received message as is
            // (persistence will be added later)

            /*
            Currently HARD CODED VALUES FOR FRONTEND TO IMPLEMENT THEIR STUFF
             */
            String[] userTiles = gameState.getUserTiles();
            String[] newTiles = new String[userTiles.length];
            for (int i = 0; i < userTiles.length; i++) {
                newTiles[i] = userTiles[i] == "" ? String.valueOf(getRandomLetter()) : userTiles[i];
            }
            gameState.setUserTiles(newTiles);
            logger.info("[LOG] Move submitted for gameId: '{}', userTiles: '{}'", gameId, gameState.getUserTiles());
            simpleMessagingTemplate.convertAndSend(
                    "/topic/game_states/" + gameId,
                    new MessageGameStateMessageDTO(
                            Long.valueOf(gameId),
                            MessageStatus.SUCCESS,
                            "Move submitted",
                            gameState
                    )
            );
            // ----------------------------------------------------------------
            return new MessageGameStateMessageDTO(
                Long.valueOf(gameId),
                MessageStatus.SUCCESS,
                "Move submitted",
                gameState
            );
        } else if (gameState.getAction().equals("EXCHANGE")) {
            // HARD CODED VALUES FOR FRONTEND TO IMPLEMENT THEIR STUFF
            int n = gameState.getUserTiles().length;
            String[] newUserTiles = new String[n];
            for (int i = 0; i < n; i++) {
                newUserTiles[i] = String.valueOf(getRandomLetter());
            }
            gameState.setUserTiles(newUserTiles);
            return new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.SUCCESS,
                    "Tiles exchanged",
                    gameState
            );
            // ---------------------------------------------------------
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


    private static char getRandomLetter() {
        Random random = new Random();
        return (char) ('A' + random.nextInt(26));
    }
}
