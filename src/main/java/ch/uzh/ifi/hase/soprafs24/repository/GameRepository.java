package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;



@Repository("gameRepository")
public interface GameRepository extends JpaRepository<Game, Long> {
    Optional <Game> findById(Long id);
    void deleteById(Long id);
}