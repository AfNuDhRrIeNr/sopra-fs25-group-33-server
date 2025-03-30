package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePutDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import java.util.Collections;
import java.util.List;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import java.util.Optional;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @MockBean
    private UserService userService;

    @Test
    public void createGame_validInput_gameCreated() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setHost("12345");
        User user = new User();
        user.setId(12345L);

        GamePostDTO gamePostDTO = new GamePostDTO();
        GameGetDTO gameGetDTO = new GameGetDTO();
        gameGetDTO.setHost("12345");


        given(gameService.createGame(Mockito.any(Game.class), Mockito.any(User.class))).willReturn(game);
        given(userService.getUserByToken("Bearer test-token")).willReturn(Optional.of(user));

        MockHttpServletRequestBuilder postRequest = post("/games")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer test-token")
            .content(asJsonString(gamePostDTO));

        mockMvc.perform(postRequest)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.host", is("12345")));

    }

    @Test
    public void createGame_invalidInput_runTimeException() throws Exception {
        GamePostDTO gamePostDTO = new GamePostDTO();

        given(userService.getUserByToken("Bearer test-token")).willReturn(Optional.empty());

        MockHttpServletRequestBuilder postRequest = post("/games")
            .contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer test-token")
            .content(asJsonString(gamePostDTO));

        mockMvc.perform(postRequest)
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void getGameById_validId_gameReturned() throws Exception {
        Game game = new Game();
        game.setId(1L);
        game.setHost("12345");

        GameGetDTO gameGetDTO = new GameGetDTO();
        gameGetDTO.setHost("12345");

        given(gameService.getGameById(1L)).willReturn(Optional.of(game));

        mockMvc.perform(get("/games/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.host", is("12345")));
    }

    @Test
    public void getGameById_invalidId_notFound() throws Exception {
        given(gameService.getGameById(1L)).willReturn(Optional.empty());

        mockMvc.perform(get("/games/1"))
            .andExpect(status().isNotFound());
    }

    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                String.format("The request body could not be created.%s", e.toString()));
        }
    }
}
