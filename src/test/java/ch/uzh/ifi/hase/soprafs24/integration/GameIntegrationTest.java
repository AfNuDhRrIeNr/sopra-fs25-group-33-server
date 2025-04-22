package ch.uzh.ifi.hase.soprafs24.integration;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class GameIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @BeforeEach // creates test user before each test
    public void setup() {

        User testUser = new User();
        testUser.setUsername("testUser");
        testUser.setToken("test-token");
        testUser.setPassword("password");
        testUser.setStatus(UserStatus.ONLINE);
        userRepository.save(testUser);
    }

    @AfterEach // deletes all created users and games after the test
    public void cleanup() {
        gameRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void createGame_validRequest_createsGameInDatabase() throws Exception {
        String token = "test-token";

        mockMvc.perform(post("/games")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated()) 
                .andExpect(jsonPath("$.gameStatus").value("CREATED"));

        List<Game> games = gameRepository.findAll();
        assertEquals(1, games.size());
        assertEquals(GameStatus.CREATED, games.get(0).getGameStatus());
        assertNotNull(games.get(0).getHost());
    }

    @Test
    public void createGame_invalidToken_returnsUnauthorized() throws Exception {
        String invalidToken = "invalid-token";

        mockMvc.perform(post("/games")
                .header("Authorization", invalidToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized()); 
    }
}