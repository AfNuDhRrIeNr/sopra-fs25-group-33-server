package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;

import java.util.ArrayList;
import java.util.List;

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
        Optional<User> user = userService.getUserByToken(token);
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid token: User not found");
        }


        Game createdGame = gameService.createGame(user.get());

        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(createdGame);
    }


    @GetMapping("/games/{id}")
    public ResponseEntity<GameGetDTO> getGameById(@PathVariable Long id) {
        return gameService.getGameById(id)
            .map(game -> ResponseEntity.ok(DTOMapper.INSTANCE.convertEntityToGameGetDTO(game)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    @PutMapping("/games/{id}")
    public ResponseEntity<GameGetDTO> updateGame(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        Optional<User> optionalUser = userService.getUserByToken(token);
        if (optionalUser.isEmpty()) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }
        User user = optionalUser.get();

        Optional<Game> gameOptional = gameService.getGameById(id);
        if (gameOptional.isEmpty()) { return ResponseEntity.notFound().build(); }
        Game game = gameOptional.get();

        if (game.getUsers().contains(user)) { return ResponseEntity.status(HttpStatus.CONFLICT).build(); }

        Game updatedGame = gameService.joinGame(game, user);
        return ResponseEntity.ok(DTOMapper.INSTANCE.convertEntityToGameGetDTO(updatedGame));
    }
}
