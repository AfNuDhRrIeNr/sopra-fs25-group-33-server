package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.constant.errors.GameNotFoundException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.InvalidGameStatusException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

import java.util.Optional;

@Service
@Transactional
public class GameService {
    private final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepository;

    private final UserRepository userRepository;

    @Autowired
    public GameService(@Qualifier("gameRepository") GameRepository gameRepository, @Qualifier("userRepository") UserRepository userRepository) {
    this.gameRepository = gameRepository;
    this.userRepository = userRepository;
    }

    public Game createGame(User host) throws UserNotFoundException {
        if(host == null || host.getId() == null || userRepository.findById(host.getId()).isEmpty()) throw new UserNotFoundException("User not found");
        Game game = new Game();
        game.setHost(host);
        game.getUsers().add(host);
        game.setGameStatus(GameStatus.CREATED);
        game.getHost().setInGame(true);
        game.getHost().setStatus(UserStatus.IN_GAME);
        return gameRepository.save(game);
    }

    public Optional<Game> getGameById(Long id) {
        return gameRepository.findById(id);
    }

    public Game joinGame(Game game, User user) throws GameNotFoundException, UserNotFoundException {
        if(game == null || game.getId() == null ||gameRepository.findById(game.getId()).isEmpty()) throw new GameNotFoundException("Game not found");
        if(user == null || user.getId() == null ||userRepository.findById(user.getId()).isEmpty()) throw new UserNotFoundException("User not found");
        game.addUser(user);
        return gameRepository.saveAndFlush(game);
    }

    public boolean isUserInGame(Game game, User user) {
        if(game == null || game.getId() == null ||gameRepository.findById(game.getId()).isEmpty()) return false;
        if(user == null || user.getId() == null ||userRepository.findById(user.getId()).isEmpty()) return false;
        return game.getUsers().contains(user);
    }

    public Game updateGameStatus(Game game, GameStatus newGameStatus) throws GameNotFoundException {
        if(game == null || game.getId() == null || gameRepository.findById(game.getId()).isEmpty()) throw new GameNotFoundException("Game not found");
        if(newGameStatus == null) throw new InvalidGameStatusException("Game status cannot be null");
        GameStatus oldStatus = game.getGameStatus();
        if((newGameStatus == GameStatus.ONGOING && oldStatus == GameStatus.TERMINATED) || newGameStatus == GameStatus.CREATED )
            throw new InvalidGameStatusException("Invalid game status transition from " + oldStatus + " to " + newGameStatus);
        game.setGameStatus(newGameStatus);
        return gameRepository.saveAndFlush(game);
    }

    public List<Character> drawLetters(Game game, int count) {
        List<Character> assignedLetters = game.drawLetters(count);
        gameRepository.saveAndFlush(game);
        return assignedLetters;
    }

    public List<Character> exchangeTiles(Game game, List<Character> tilesToExchange) {
        List<Character> exchangedTiles = game.exchangeTiles(tilesToExchange);
        gameRepository.saveAndFlush(game);
        return exchangedTiles;
    }

    public boolean skipTurn(Long id, Long playerId) throws GameNotFoundException {
        Optional<Game> gameOptional = gameRepository.findById(id);
        if (gameOptional.isPresent()) {
            Game game = gameOptional.get();
            game.setHostTurn(!game.getHost().getId().equals(playerId));
            gameRepository.save(game);
            return game.isHostTurn();
        } else {
            log.warn("Game with id {} not found", id);
            throw new GameNotFoundException("Game not found");
        }
    }

    public int countLettersInBag (Game game, Character letter) {
        int letterCount = game.getRemainingLetterCount(letter);
        return letterCount;
    }

    public void deleteGame(Game game) {
        gameRepository.delete(game);
    }
}
