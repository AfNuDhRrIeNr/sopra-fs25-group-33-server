package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.constant.errors.GameInvitationNotFoundException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.GameNotFoundException;
import ch.uzh.ifi.hase.soprafs24.constant.errors.UserNotFoundException;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameInvitation;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.enums.InvitationStatus;
import ch.uzh.ifi.hase.soprafs24.repository.GameInvitationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class GameInvitationService {
        private final Logger log = LoggerFactory.getLogger(GameInvitationService.class);
        private final GameService gameService;

        private final UserService userService;

        private final GameInvitationRepository gameInvitationRepository;
        @Autowired
        public GameInvitationService(@Qualifier("gameService") GameService gameService, @Qualifier("userService") UserService userService, @Qualifier("gameInvitationRepository") GameInvitationRepository gameInvitationRepository) {
            this.gameService = gameService;
            this.userService = userService;
            this.gameInvitationRepository = gameInvitationRepository;
        }

        public GameInvitation getGameInvitationById(Long id) throws GameInvitationNotFoundException {
            if(id == null) throw new IllegalArgumentException("Id cannot be null");
            return gameInvitationRepository.findById(id)
                    .orElseThrow(() -> new GameInvitationNotFoundException("Game invitation with id "+id.toString()+" not found"));
        }

        public List<GameInvitation> getGameInvitationsByTarget(User sender) {
            if(sender == null || sender.getId() == null) throw new IllegalArgumentException("Sender cannot be null");
            return gameInvitationRepository.findAllByTarget(sender);
        }



    public GameInvitation createGameInvitation(Optional<Game> optionalGame, Optional<User> optionalSender, Optional<User> optionalTarget) throws GameNotFoundException, UserNotFoundException {

        if(optionalGame == null || optionalGame.isEmpty() || optionalGame.get().getId() == null || gameService.getGameById(optionalGame.get().getId()).isEmpty()) throw new GameNotFoundException("Game not found");
        if(optionalSender == null || optionalSender.isEmpty() || optionalSender.get().getId() == null || userService.getUserById(optionalSender.get().getId()).isEmpty()) throw new UserNotFoundException("User not found");
        if(optionalTarget == null || optionalTarget.isEmpty() ||optionalTarget.get().getId() == null || userService.getUserById(optionalTarget.get().getId()).isEmpty()) throw new UserNotFoundException("User not found");
        Game game = optionalGame.get();
        User sender = optionalSender.get();
        User target = optionalTarget.get();
        if(gameInvitationRepository.findByGameAndTarget(game, target).isPresent()) throw new IllegalArgumentException("Game invitation already exists");


        GameInvitation gameInvitation = new GameInvitation();
        gameInvitation.setGame(game);
        gameInvitation.setSender(sender);
        gameInvitation.setTarget(target);
        gameInvitation.setStatus(InvitationStatus.PENDING);
        gameInvitation.setTimeStamp(LocalDateTime.now());
        return gameInvitationRepository.saveAndFlush(gameInvitation);
    }

    public GameInvitation updateGameInvitationStatus(GameInvitation gameInvitation, InvitationStatus status) throws UserNotFoundException, GameNotFoundException {
        if(gameInvitation == null || gameInvitation.getId() == null) throw new IllegalArgumentException("Game invitation cannot be null");
        if(status == null) throw new IllegalArgumentException("Status cannot be null");
        if(gameInvitation.getGame() == null || gameInvitation.getGame().getId() == null || gameService.getGameById(gameInvitation.getGame().getId()).isEmpty()) throw new GameNotFoundException("Game not found");
        if(gameInvitation.getSender() == null || gameInvitation.getSender().getId() == null || userService.getUserById(gameInvitation.getSender().getId()).isEmpty()) throw new UserNotFoundException("Sender not found");
        if(gameInvitation.getTarget() == null || gameInvitation.getTarget().getId() == null || userService.getUserById(gameInvitation.getTarget().getId()).isEmpty()) throw new UserNotFoundException("Target not found");
        if(gameInvitation.getGame().getUsers().size() >= 2) throw new IllegalArgumentException("Game is already full");
        gameInvitation.setStatus(status);
        if(status==InvitationStatus.ACCEPTED) {
            userService.updateUserStatus(gameInvitation.getTarget(), UserStatus.IN_GAME);
            gameService.joinGame(gameInvitation.getGame(), gameInvitation.getTarget());
        }
        return gameInvitationRepository.saveAndFlush(gameInvitation);
    }
}
