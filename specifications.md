# ScrabbleNow API Specifications

## REST API Endpoints

| Mapping               | Method     | Parameter              | Parameter Type | Status Code  | Response                                        | Description                  |
|-----------------------|------------|------------------------|----------------|--------------|-------------------------------------------------|------------------------------|
| `/users`              | GET        | filter                 | Body           | 200/404      | List\<User\>                                      | Retrieve User(s)             |
| `/users/register`           | POST       | username, password     | Body           | 201/409      | User                                            | Add User                     |
| `/users/login`              | POST       | username, password     | Body           | 200/401      | User                                            | Authorize User               |
| `/users/logout`             | PUT        | token                  | Header         | 204/404      |  -                                              | Updates User Status          |
| `/users/{userId}`     | GET        | userId                 | Query          | 200/404      | User                                            | Retrieve User Profile        |
| `/users/{userId}`     | PUT        | User                   | Body           | 200/404      |  -                                               | Update User Profile          |
| `/users/friendRequests/{userId}`| GET| userId               | Query          | 200/404      | List\<FriendRequests\>                            | Get Friend requests for a User|
| `/users/friendRequests`| POST      |  FriendRequest         | Body           | 201/404      | FriendRequest                                   | Send new friend request     |
| `/users/friendRequest/{requestId}` | PUT | requestId, status      | Query, Body    | 200/404      | FriendRequestStatus                             | Update status of a request|
| `/games`              | POST       | userId                 | Body           | 201/409      | Game                                            | Instantiate new Game        |
| `/games`              | PUT        | Game                   | Body           | 200/404      | -                                             | Update Game (players, status)     |
| `/games/letters`      |GET         | gameId, userId, lettter   | Body           | 200/401/404  | `{"A": 5}`     | Get amount of tiles left of a game for a certain user and letter |
| `/games/invitations`  | POST       | GameInvitation         | Body           | 201/404      | GameInvitation                             | Create a new game invitation       |
| `/games/invitations/{userId}` | GET| userId                 | Query          | 200/404      | List\<GameInvitations\>                      | Get all game invitations for a user|
| `games/invitations/{invitaionId}` | PUT | invitationId, status | Query, Body | 200/404      | -                                          | Change the invitation status |

---

## WebSocket API (using STOMP)

| Destination                                 | Method      | Parameter  | Parameter Type | Response  | Description |
|-----------------------------------------|------------|------------|---------------|-----------|-------------|
| `/connections`                          | SUBSCRIBE| -          | -             | `CONNECTED` | Initial WebSocket handshake |
| `/moveValidator/{userId}`               | SEND     | moveData | body        | -         | Sends desired move for validation |
| `/queue/moveValidator/{userId}`         | SUBSCRIBE| -          | -             | `{"valid": true, "message": "Move accepted"}` | Receives response for a move validation |
| `/gameStates/{gameId}`                  | SEND     | moveData | body        | -         | Commits a move |
| `/topic/gameStates/{gameId}`            | SUBSCRIBE| -          | -             | GameState | Waits for new game states |
