package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
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

import java.util.Collections;
import java.util.List;

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

    @Test
    public void createGame_validInput_gameCreated() throws Exception {
    
        Game game = new Game(); 
        game.setId(1L); 

        GamePostDTO gamePostDTO = new GamePostDTO();
        GameGetDTO gameGetDTO = new GameGetDTO();
        gameGetDTO.setHost("12345"); 
        gameGetDTO.setBoardBase("DEFAULT_BOARD"); 
        gameGetDTO.setGameStatus("WAITING"); 

        given(gameService.createGame(Mockito.any())).willReturn(game);
        given(DTOMapper.INSTANCE.convertEntityToGameGetDTO(Mockito.any())).willReturn(gameGetDTO);

        MockHttpServletRequestBuilder postRequest = post("/games")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(gamePostDTO));

        mockMvc.perform(postRequest)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.host", is(gameGetDTO.getHost())))
            .andExpect(jsonPath("$.boardBase", is(gameGetDTO.getBoardBase())))
            .andExpect(jsonPath("$.gameStatus", is(gameGetDTO.getGameStatus())));
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
