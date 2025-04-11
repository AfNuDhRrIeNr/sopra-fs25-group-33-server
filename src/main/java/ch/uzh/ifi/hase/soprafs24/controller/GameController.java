package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.errors.InvalidGameStatusException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;

import java.util.Optional;

@RestController
public class GameController {

    private final GameService gameService;
    private final UserService userService;
    @Autowired
    GameController(GameService gameService, UserService userService) {
      this.gameService = gameService;
      this.userService = userService;
    }

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public GameGetDTO createGame( @RequestHeader("Authorization") String token) {
        System.out.println(token);
        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // Remove "Bearer " (7 characters)
        }
        System.out.println(token);
        Optional<User> user = userService.getUserByToken(token);
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid token: User not found");
        }

        try {
            Game createdGame = gameService.createGame(user.get());
            return DTOMapper.INSTANCE.convertEntityToGameGetDTO(createdGame);
        } catch (UserNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred:\n"+exception.getMessage());
        }
    }


    @GetMapping("/games/{id}")
    public ResponseEntity<GameGetDTO> getGameById(@PathVariable Long id) {
        return gameService.getGameById(id)
            .map(game -> ResponseEntity.ok(DTOMapper.INSTANCE.convertEntityToGameGetDTO(game)))
            .orElse(ResponseEntity.notFound().build());
    }


    @GetMapping("/gamestate/users/{userId}/remaining/{letter}")
    public ResponseEntity<Map<String, Object>> getRemaining(
            @PathVariable String userId,
            @PathVariable String letter) {
        
        int remaining = 11; // get the value from your dictionary
        Map<String, Object> response = new HashMap<>();
        response.put("letter", letter);
        response.put("remaining", remaining);
    
        return ResponseEntity.ok(response);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/games/{id}")
    public ResponseEntity<GameGetDTO> updateGame(@PathVariable Long id, @RequestHeader("Authorization") String token, @RequestBody GamePutDTO gamePutDTO) {

        Optional<User> optionalUser = userService.getUserByToken(token);
        if (optionalUser.isEmpty()) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }
        User user = optionalUser.get();

        Optional<Game> gameOptional = gameService.getGameById(id);
        if (gameOptional.isEmpty()) { return ResponseEntity.notFound().build(); }
        Game game = gameOptional.get();

        if (!gameService.isUserInGame(game,user)) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }

        if (gamePutDTO.getGameStatus() != null)
            try {
                gameService.updateGameStatus(game, gamePutDTO.getGameStatus());
            } catch (InvalidGameStatusException e) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An unexpected error occurred:\n"+e.getMessage());
            }
        return ResponseEntity.ok(DTOMapper.INSTANCE.convertEntityToGameGetDTO(game));
    }
}
