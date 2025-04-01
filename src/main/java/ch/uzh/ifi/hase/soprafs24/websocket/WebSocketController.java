package ch.uzh.ifi.hase.soprafs24.websocket;

import ch.uzh.ifi.hase.soprafs24.constant.MessageStatus;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MessageGameStateMessageDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStateDTO;
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
    SimpMessagingTemplate simpleMessagingTemplate;

    private static final Logger logger = LoggerFactory.getLogger(WebSocketController.class);


    // ------------------ Game State ---------------------------------------
    @MessageMapping("/game_states/{gameId}")
    @SendTo("/topic/game_states/{gameId}")
    public void handleGameStates(@DestinationVariable String gameId, GameStateDTO gameState) {
        logger.debug("[LOG] Game endpoint reached with gameId: '{}' and gameState entity: '{}'",gameId, gameState.toString());
        // Verify DTO
        try {
            gameState.isValid();
        } catch (IllegalArgumentException exception) {
            simpleMessagingTemplate.convertAndSend("/topic/game_states/"+gameId, new MessageGameStateMessageDTO(
                    Long.valueOf(gameId),
                    MessageStatus.ERROR,
                    exception.getMessage(),
                    null
            ));
        }
        if(gameState.getId() != Long.valueOf(gameId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"The game id of the object and destination are not equal!");
        }

        // verify words of game

        // Add new gameState no database (if submit)

        // Send gameState back to users



        simpleMessagingTemplate.convertAndSend("/topic/game_states/"+gameId, new MessageGameStateMessageDTO(
                Long.valueOf(gameId),
                MessageStatus.SUCCESS,
                "GameState successfully received",
                gameState
        ));
    }



    // ------------------ Moves ---------------------------------------------

}
