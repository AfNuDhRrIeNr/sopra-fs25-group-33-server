# ScrabbleNow API Specifications

## REST API Endpoints

| Mapping               | Method     | Parameter              | Parameter Type | Status Code  | Response                                        | Description                  |
|-----------------------|------------|------------------------|----------------|--------------|-------------------------------------------------|------------------------------|
| `/users`              | GET        | filter                 | Body           | 200/404      | User-Array                                      | Retrieve User(s)             |
| `/register`           | POST       | username, password     | Body           | 201/409      | User                                            | Add User                     |
| `/login`              | POST       | username, password     | Body           | 200/401      | User                                            | Authorize User               |
| `/logout`             | PUT        | token                  | Header         | 200/404      |                                                 | Updates User Status          |
| `/users/{userId}`     | GET        | userID                 | Query          | 200/404      | User                                            | Retrieve User Profile        |
| `/users/{userId}`     | PUT        | userID                 | Body           | 200/404      |                                                 | Update User Profile          |
| `/games`              | POST       | userID                 | Body           | 201/409      | Game                                            | Instantiate new Game        |
| `/games`              | PUT        | Game                   | Body           | 200/404      | Game                                            | Update Game                 |
| `/games/starter`      | PUT        | -                      | -              | 200/404      | `{"gameId": "1234", "status": "started"}`       | Initialize WebSocket session |
| `/games/finisher`     | PUT        | gameID                 | Body           | 200/404      | `{"gameId": "1234", "status": "finished"}`      | Finish Game                  |


---

## WebSocket Events

| Mapping                                 | Method      | Parameter  | Parameter Type | Response  | Description |
|-----------------------------------------|------------|------------|---------------|-----------|-------------|
| `/connections`                          | SUBSCRIBE| -          | -             | `CONNECTED` | Initial WebSocket handshake |
| `/moveValidator/{userId}`               | SEND     | moveData | body        | -         | Sends desired move for validation |
| `/queue/moveValidator/{userId}`         | SUBSCRIBE| -          | -             | `{"valid": true, "message": "Move accepted"}` | Receives response for a move validation |
| `/gameStates/{gameId}`                  | SEND     | moveData | body        | -         | Sends a move |
| `/topic/gameStates/{gameId}`            | SUBSCRIBE| -          | -             | `{"gameId": "1234", "state": "updated"}` | Waits for new game states |
| `/gameStates/{gameId}/finish`           | SEND     | gameId   | path        | -         | Sends request to finish game |
| `/topic/gameStates/{gameId}/finish`     | SUBSCRIBE| -          | -             | `{"gameId": "1234", "status": "finished"}` | Clients receive message that game ended |