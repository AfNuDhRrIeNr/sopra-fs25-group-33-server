package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.GameInvitation;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository("gameInvitationRepository")
public interface GameInvitationRepository extends JpaRepository<GameInvitation, Long> {

        Optional<GameInvitation> findById(Long id);

        Optional<GameInvitation> findByGame(Game game);

        Optional<GameInvitation> findBySender(User sender);

        Optional<GameInvitation> findByTarget(User target);

        Optional<GameInvitation> findByGameAndTarget(Game game, User target);


        @Query("SELECT gi FROM GameInvitation gi WHERE gi.sender.id = :senderId")
        Optional<GameInvitation> findBySenderId(@Param("senderId") Long senderId);

        @Query("SELECT gi FROM GameInvitation gi WHERE gi.target.id = :targetId")
        Optional<GameInvitation> findByTargetId(@Param("targetId") Long targetId);

        @Query("SELECT gi FROM GameInvitation gi WHERE gi.game.id = :gameId")
        Optional<GameInvitation> findByGameId(@Param("gameId") Long gameId);
}


