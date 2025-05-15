package ch.uzh.ifi.hase.soprafs24.websocket;

import ch.uzh.ifi.hase.soprafs24.constant.MessageStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameStateDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.MessageGameStateMessageDTO;
import ch.uzh.ifi.hase.soprafs24.service.MoveSubmitService;
import ch.uzh.ifi.hase.soprafs24.service.MoveValidatorService;
import org.junit.jupiter.api.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSession.Subscription;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class WebSocketControllerTest {

    @LocalServerPort
    private int port;

    private String wsConnectionUrl;
    private static WebSocketStompClient client;

    private static final String SUBSCRIBE_GAME_STATE_DESTINATION = "/topic/game_states/";

    private CompletableFuture<MessageGameStateMessageDTO> completableFuture;
    private StompSession session;
    private Subscription subscription;

    @Autowired
    private WebSocketController webSocketController;

    @MockBean
    private MoveSubmitService moveSubmitService;

    @MockBean
    private GameRepository gameRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private MoveValidatorService moveValidatorService;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @BeforeAll
    public void setUpOnce() {
        client = new WebSocketStompClient(new SockJsClient(createTransportClient()));
        client.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @BeforeEach
    public void setUp() {
        completableFuture = new CompletableFuture<>();
        String webSocketBaseUrl = "ws://localhost:" + port;
        wsConnectionUrl = webSocketBaseUrl + "/connections";
    }

    @AfterEach
    public void tearDown() {
        if(client.isRunning() && subscription != null && session.isConnected()
        ) subscription.unsubscribe();
        if(client.isRunning() && session.isConnected()) session.disconnect();
    }

    @Test
    public void testConnectToWebSocketSuccessful() throws ExecutionException, InterruptedException, TimeoutException {
        this.session = client.connect(this.wsConnectionUrl, new StompSessionHandlerAdapter() {})
                .get(10, TimeUnit.SECONDS);

        assertNotNull(session);
        assertTrue(session.isConnected(), "WebSocket session should be connected");
    }

    @Test
    public void SubscribeToGameStates_SuccessfulSubscription_returnsGameStates()
            throws ExecutionException, InterruptedException, TimeoutException {
        // Assign
        completableFuture = new CompletableFuture<>();
        StompFrameHandler stompFrameHandler = new MessageGameStateDTOHandler(completableFuture);
        session = client.connect(this.wsConnectionUrl, new StompSessionHandlerAdapter() {})
                .get(10, TimeUnit.SECONDS);

        assertNotNull(session);
        assertTrue(session.isConnected(), "WebSocket session should be connected");

        Long gameId = 1L;

        // Act 1: Subscribe
        subscription = session.subscribe(SUBSCRIBE_GAME_STATE_DESTINATION + gameId, stompFrameHandler);

        // Assert 1
        assertNotNull(subscription);
        assertNotNull(subscription.getSubscriptionId());

        // Simulate message reception
        MessageGameStateMessageDTO testMessage = new MessageGameStateMessageDTO(
                gameId, MessageStatus.SUCCESS, "GameState successfully received", new GameStateDTO()
        );

        // Act 2: Manually trigger the handler
        stompFrameHandler.handleFrame(new StompHeaders(), testMessage);

        // Assert 2
        MessageGameStateMessageDTO msg = completableFuture.get(2, TimeUnit.SECONDS);
        assertTrue(completableFuture.isDone(), "CompletableFuture should be done");
        assertNotNull(msg);
        assertEquals(gameId, msg.getGameId());
        assertEquals(MessageStatus.SUCCESS, msg.getMessageStatus());
        assertEquals("GameState successfully received", msg.getMessage());
        assertNotNull(msg.getGameState());
    }

    /**
     * Test multiple subscriptions receive the same message
     */
    @Test
    public void testMultipleSubscriptions_receiveSameMessage() throws Exception {
        Long gameId = 1L;
        session = client.connect(this.wsConnectionUrl, new StompSessionHandlerAdapter() {})
                .get(10, TimeUnit.SECONDS);

        CompletableFuture<MessageGameStateMessageDTO> completableFuture1 = new CompletableFuture<>();
        CompletableFuture<MessageGameStateMessageDTO> completableFuture2 = new CompletableFuture<>();

        // Subscribe twice
        Subscription subscription1 = session.subscribe(SUBSCRIBE_GAME_STATE_DESTINATION + gameId, new MessageGameStateDTOHandler(completableFuture1));
        Subscription subscription2 = session.subscribe(SUBSCRIBE_GAME_STATE_DESTINATION + gameId, new MessageGameStateDTOHandler(completableFuture2));

        assertNotNull(subscription1, "First subscription should be successful");
        assertNotNull(subscription2, "Second subscription should be successful");

        // Simulate receiving a message
        MessageGameStateMessageDTO testMessage = new MessageGameStateMessageDTO(
                gameId,
                MessageStatus.SUCCESS,
                "GameState successfully received",
                new GameStateDTO()
        );

        // Manually trigger message handlers
        new MessageGameStateDTOHandler(completableFuture1).handleFrame(new StompHeaders(), testMessage);
        new MessageGameStateDTOHandler(completableFuture2).handleFrame(new StompHeaders(), testMessage);

        // Verify both subscriptions received the same message
        MessageGameStateMessageDTO msg1 = completableFuture1.get(2, TimeUnit.SECONDS);
        MessageGameStateMessageDTO msg2 = completableFuture2.get(2, TimeUnit.SECONDS);

        assertNotNull(msg1, "First subscriber should receive the message");
        assertNotNull(msg2, "Second subscriber should receive the message");

        assertEquals(msg1.getGameId(), gameId, "Game ID should match");
        assertEquals(msg2.getGameId(), gameId, "Game ID should match");

        assertEquals(msg1.getMessage(), "GameState successfully received", "Message content should match");
        assertEquals(msg2.getMessage(), "GameState successfully received", "Message content should match");

        assertEquals(msg1.getMessageStatus(), MessageStatus.SUCCESS, "Status should be SUCCESS");
        assertEquals(msg2.getMessageStatus(), MessageStatus.SUCCESS, "Status should be SUCCESS");
    }

    @Test
    void testValidateMove_gameNotFound_sendsErrorToUserChannel() {
        // Given
        Long gameId = 1L;
        Long playerId = 123L;

        GameStateDTO gameState = new GameStateDTO();
        gameState.setId(gameId);
        gameState.setPlayerId(playerId);
        gameState.setAction("VALIDATE");
        gameState.setToken("test-token");
        gameState.setUserTiles(new String[]{"A", "B", "C", "D", "E"});

        
        String[][] board = new String[15][15];
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                board[i][j] = "";
            }
        }

        board[7][7] = "H";
        board[7][8] = "E";
        board[7][9] = "L";
        board[7][10] = "L";
        board[7][11] = "O";
        gameState.setBoard(board);

        //when
        when(moveValidatorService.validateMoveAndExtractWords(eq(gameId), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));


        MessageGameStateMessageDTO result = webSocketController.handleGameStates(
                gameId.toString(), gameState);


        assertNull(result);

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MessageGameStateMessageDTO> messageCaptor = ArgumentCaptor.forClass(MessageGameStateMessageDTO.class);

        verify(messagingTemplate).convertAndSend(
                destinationCaptor.capture(), messageCaptor.capture());

        assertEquals("/topic/game_states/users/" + playerId, destinationCaptor.getValue());

        //then
        MessageGameStateMessageDTO sentMessage = messageCaptor.getValue();
        assertEquals(gameId, sentMessage.getGameId());
        assertEquals(MessageStatus.VALIDATION_ERROR, sentMessage.getMessageStatus());
        assertEquals("404 NOT_FOUND \"Game not found\"", sentMessage.getMessage());
    }

    private static List<Transport> createTransportClient() {
        return List.of(new WebSocketTransport(new StandardWebSocketClient()));
    }

    private class MessageGameStateDTOHandler implements StompFrameHandler {
        private final CompletableFuture<MessageGameStateMessageDTO> completableFuture;
        public MessageGameStateDTOHandler(CompletableFuture<MessageGameStateMessageDTO> completableFuture) {
            this.completableFuture = completableFuture;
        }
        @Override
        public Type getPayloadType(StompHeaders stompHeaders) {
            return MessageGameStateMessageDTO.class;
        }

        @Override
        public void handleFrame(StompHeaders stompHeaders, Object o) {
            completableFuture.complete((MessageGameStateMessageDTO) o);
        }
    }

    @Test
    void testSubmitMove_success() {
        // Given
        Long gameId = 1L;
        Long playerId = 123L;

        GameStateDTO gameState = new GameStateDTO();
        gameState.setId(gameId);
        gameState.setPlayerId(playerId);
        gameState.setAction("SUBMIT");
        gameState.setToken("test-token");
        gameState.setUserTiles(new String[]{"A", "B", "C", "D", "E"});
        
        String[][] board = new String[15][15];
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                board[i][j] = "";
            }
        }
        board[7][7] = "H";
        board[7][8] = "E";
        board[7][9] = "L";
        board[7][10] = "L";
        board[7][11] = "O";
        gameState.setBoard(board);

        // Mock services
        int expectedScore = 10;
        when(moveSubmitService.submitMove(eq(gameId), any())).thenReturn(expectedScore);
        
        // Create mock game object with player scores
        Game mockGame = Mockito.mock(Game.class);
        Map<Long, Integer> playerScores = new HashMap<>();
        playerScores.put(playerId, expectedScore);
        when(mockGame.getPlayerScores()).thenReturn(playerScores);
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(mockGame));

        // When
        MessageGameStateMessageDTO result = webSocketController.handleGameStates(
                gameId.toString(), gameState);

        // Then
        assertNotNull(result);
        assertEquals(gameId, result.getGameId());
        assertEquals(MessageStatus.SUCCESS, result.getMessageStatus());
        assertEquals("Move submitted, scored " + expectedScore + " points", result.getMessage());
        
        // Verify player score was updated
        verify(mockGame).addScore(playerId, expectedScore);
        verify(gameRepository).save(mockGame);
    }
    @Test
    void handleGameStates_gameEnd_setsGameTerminatedAndUpdatesUsers() {
        Long gameId = 1L;
        Long playerId = 123L;

        GameStateDTO gameState = new GameStateDTO();
        gameState.setId(gameId);
        gameState.setPlayerId(playerId);
        gameState.setAction("GAME_END");
        gameState.setToken("test-token");
        gameState.setUserTiles(new String[]{"A", "B", "C", "D", "E"});
        
        String[][] board = new String[15][15];
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                board[i][j] = "";
            }
        }
        board[7][7] = "H";
        board[7][8] = "E";
        board[7][9] = "L";
        board[7][10] = "L";
        board[7][11] = "O";
        gameState.setBoard(board);


        User user1 = new User();
        user1.setId(10L);
        user1.setHighScore(5);
        user1.setInGame(true);
    
        User user2 = new User();
        user2.setId(20L);
        user2.setHighScore(3);
        user2.setInGame(true);

        Game game = Mockito.mock(Game.class);
        when(game.getUsers()).thenReturn(List.of(user1, user2));
        when(game.getPlayerScores()).thenReturn(Map.of(10L, 8, 20L, 4));
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));

        webSocketController.handleGameStates(gameId.toString(), gameState);

        verify(game).setGameStatus(GameStatus.TERMINATED);
        verify(gameRepository).saveAndFlush(game);
        verify(userRepository, times(2)).saveAndFlush(any(User.class));

        verify(messagingTemplate).convertAndSend(
            eq("/topic/game_states/" + gameId),
            any(MessageGameStateMessageDTO.class)
        );

    }
    @Test
    void handleGameStates_surrender_sendsSurrenderMessage() {
        Long gameId = 1L;
        Long playerId = 123L;

        GameStateDTO gameState = new GameStateDTO();
        gameState.setId(gameId);
        gameState.setPlayerId(playerId);
        gameState.setAction("SURRENDER");
        gameState.setToken("test-token");
        gameState.setUserTiles(new String[]{"A", "B", "C", "D", "E"});
        
        String[][] board = new String[15][15];
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                board[i][j] = "";
            }
        }
        board[7][7] = "H";
        board[7][8] = "E";
        board[7][9] = "L";
        board[7][10] = "L";
        board[7][11] = "O";
        gameState.setBoard(board);

        User user1 = new User();
        user1.setId(10L);
        user1.setHighScore(5);
        user1.setInGame(true);
    
        User user2 = new User();
        user2.setId(20L);
        user2.setHighScore(3);
        user2.setInGame(true);

        Game game = Mockito.mock(Game.class);
        when(game.getUsers()).thenReturn(List.of(user1, user2));
        when(game.getPlayerScores()).thenReturn(Map.of(10L, 8, 20L, 4));
        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));

        when(gameRepository.findById(gameId)).thenReturn(Optional.of(game));

        webSocketController.handleGameStates(gameId.toString(), gameState);

        verify(game).setGameStatus(GameStatus.TERMINATED);
        verify(gameRepository).saveAndFlush(game);
    
        verify(messagingTemplate).convertAndSend(
            eq("/topic/game_states/" + gameId),
            any(MessageGameStateMessageDTO.class)
        );
    }
}
