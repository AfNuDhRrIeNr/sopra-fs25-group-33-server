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
import org.springframework.beans.factory.annotation.Autowired;
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
    public GameGetDTO createGame(@RequestBody GamePostDTO gamePostDTO, @RequestHeader("Authorization") String token) {
        Optional<User> user = userService.getUserByToken(token);
        if (user.isEmpty()) {
            throw new RuntimeException("Invalid token: User not found");
        }
        Game gameInput = DTOMapper.INSTANCE.convertGamePostDTOtoEntity(gamePostDTO);

        gameInput.setHost(user.get().getId().toString());

        Game createdGame = gameService.createGame(gameInput);

        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(createdGame);
    }
    
}
