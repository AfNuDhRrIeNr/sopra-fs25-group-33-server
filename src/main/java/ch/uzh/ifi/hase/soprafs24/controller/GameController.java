package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.errors.GameInvitationNotFoundException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.GameNotFoundException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.InvalidGameStatusException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameInvitation;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameInvitationService;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class GameController {

    private final GameService gameService;
    private final UserService userService;

    private final GameInvitationService gameInvitationService;
    @Autowired
    GameController(GameService gameService, UserService userService, GameInvitationService gameInvitationService) {
      this.gameService = gameService;
      this.userService = userService;
      this.gameInvitationService = gameInvitationService;
    }

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public GameGetDTO createGame( @RequestHeader("Authorization") String token) {
        Optional<User> user = userService.getUserByToken(token);
        if (user.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid token: User not found" + token);
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

    @ResponseStatus(HttpStatus.OK)
    @DeleteMapping("/games/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long id, @RequestHeader("Authorization") String token) {
        Optional<User> optionalUser = userService.getUserByToken(token);
        if (optionalUser.isEmpty()) { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }
        
        Game game = gameService.getGameById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        if (!game.getUsers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete game: Users are still in the game");
        }
        gameService.deleteGame(game);

        return ResponseEntity.noContent().build();
    }



    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/games/{id}/assign")
    public GamePutDTO assignTiles(@PathVariable Long id, @RequestParam int count) {
        Game game = gameService.getGameById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        List<Character> newTiles =gameService.drawLetters(game, count);
        GamePutDTO response = new GamePutDTO();
        response.setNewTiles(newTiles);
        return response;
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/games/{id}/exchange")
    public GamePutDTO exchangeTiles(@PathVariable Long id, @RequestBody List<Character> tilesForExchange) {
        Game game = gameService.getGameById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        List<Character> newTiles = gameService.exchangeTiles(game, tilesForExchange);
        GamePutDTO response = new GamePutDTO();
        response.setNewTiles(newTiles);
        return response;
    }

    @GetMapping("/games/{id}/letters/{letter}")
    public int getRemainingLetters(@PathVariable Long id, @PathVariable char letter) {
        Game game = gameService.getGameById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));
        return gameService.countLettersInBag(game, letter);
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/games/invitations")
    public ResponseEntity<GameInvitationsGetDTO> createGameInvitation(@RequestHeader("Authorization") String token, @RequestBody GameInvitationPostDTO gameInvitationPostDTO) {
        Optional<User> senderUser = userService.getUserByToken(token);
        if (senderUser.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token: User not found");

        User targetUser = userService.getUserByUsername(gameInvitationPostDTO.getTargetUsername());
        if (targetUser == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found");

        if(senderUser.get().getId().equals(targetUser.getId())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot send game invitation to yourself!");

        Optional<Game> game = gameService.getGameById(gameInvitationPostDTO.getGameId());

        GameInvitation gameInvitation = null;
        try {
            gameInvitation = gameInvitationService.createGameInvitation(game,senderUser,Optional.of(targetUser));
        }
        catch (GameNotFoundException | UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred:\n"+e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(DTOMapper.INSTANCE.convertEntityToGameInvitationsGetDTO(gameInvitation));
    }

    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/games/invitations/{userId}")
    public ResponseEntity<List<GameInvitationsGetDTO>> getGameInvitations(@RequestHeader("Authorization") String token, @PathVariable Long userId) {
        Optional<User> user = userService.getUserByToken(token);
        if (user.isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token: User not found");

        if (!user.get().getId().equals(userId))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token: User not authorized to access this resource");
        List<GameInvitation> gameInvitations = gameInvitationService.getGameInvitationsByTarget(user.get());
        List<GameInvitationsGetDTO> gameInvitationsGetDTOs = new ArrayList<>();

        for (GameInvitation gameInvitation : gameInvitations) {
            gameInvitationsGetDTOs.add(DTOMapper.INSTANCE.convertEntityToGameInvitationsGetDTO(gameInvitation));
        }
        return ResponseEntity.ok(gameInvitationsGetDTOs);
    }

    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/games/invitations/{invitationId}")
    public ResponseEntity<GameInvitationsGetDTO> updateGameInvitations(@RequestHeader("Authorization") String token, @PathVariable String invitationId, @RequestBody GameInvitationPutDTO gameInvitationPutDTO) throws GameInvitationNotFoundException {
        if(token == null || token.isEmpty() || userService.getUserByToken(token).isEmpty()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token: User not found");
        if(invitationId == null || invitationId.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation ID cannot be null or empty");
        if(gameInvitationPutDTO == null || gameInvitationPutDTO.getStatus() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invitation status cannot be null");
        if(userService.getUserByToken(token).get().getId() != gameInvitationService.getGameInvitationById(Long.valueOf(invitationId)).getTarget().getId()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token: User not authorized to access this resource");

        try {
            GameInvitation gameInvitation = gameInvitationService.getGameInvitationById(Long.valueOf(invitationId));
            gameInvitationService.updateGameInvitationStatus(gameInvitation, gameInvitationPutDTO.getStatus());
        }
        catch (GameInvitationNotFoundException | GameNotFoundException | UserNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
        catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage());
        }
        catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred:\n" + e.getMessage());
        }
        return ResponseEntity.ok(DTOMapper.INSTANCE.convertEntityToGameInvitationsGetDTO(gameInvitationService.getGameInvitationById(Long.valueOf(invitationId))));
    }

    @PutMapping("/{gameId}/startTime")
    public ResponseEntity<Game> setGameStartTime(@PathVariable Long gameId) {
        Game updatedGame = gameService.setGameStartTime(gameId);
        return ResponseEntity.ok(updatedGame);
    }

}
