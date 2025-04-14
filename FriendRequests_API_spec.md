# 📬 FriendRequest API Specification

This document outlines the RESTful API for managing friend requests between users in the system.

---

## 🔹 GET `/users/friendrequests`

Retrieve all friend requests for the authenticated user.

- **URL:** `http://localhost:8080/users/friendRequests`
- **Method:** `GET`
- **Headers:**
    - `Authorization: <token>`

### ✅ Response

```json
[
  {
    "id": 1,
    "sender": {
      "id": 1,
      "username": "luca",
      "status": "ONLINE",
      "token": "2ef0138a-9a44-455d-b814-0d2b7c0a7987",
      "bestGamePlayed": null,
      "friends": [],
      "inGame": false
    },
    "target": {
      "id": 2,
      "username": "luca2",
      "status": "ONLINE",
      "token": "83e09994-57a5-4604-b7bf-3cca114ac4ad",
      "bestGamePlayed": null,
      "friends": [],
      "inGame": false
    },
    "status": "PENDING",
    "timeStamp": "2025-04-14T13:48:02.528829",
    "message": "hello world"
  }
]
```
###  ❌ Error Responses
- **401 Unauthorized**:
    - empty token or no user found to token
- **500 Internal Server Error**:
    - Unexpected error


## 🔹 POST `/users/friendrequests`

Retrieve all friend requests for the authenticated user.

- **URL:** `http://localhost:8080/users/friendRequests`
- **Method:** `POST`
- **Headers:**
    - `Authorization: <token>`
- **Request-Body**:

```json
{
    "message": "hello world message",
    "targetId": 1
}
```

### ✅ Response

```json
{
    "id": 1,
    "sender": {
        "id": 2,
        "username": "luca",
        "status": "ONLINE",
        "token": "0c7e48b4-125c-47de-a476-0ecc15d00796",
        "bestGamePlayed": null,
        "friends": [],
        "inGame": false
    },
    "target": {
        "id": 1,
        "username": "luca2",
        "status": "ONLINE",
        "token": "e06a3d14-c13c-46d8-9d6f-69e0d836f753",
        "bestGamePlayed": null,
        "friends": [],
        "inGame": false
    },
    "status": "PENDING",
    "timeStamp": "2025-04-14T13:41:17.5089143",
    "message": "hello world"
}
```
###  ❌ Error Responses
- **400 Bad Request**:
    - TargetId is null
    - Request is sent from same user to himself
- **401 Unauthorized**:
    - empty token or no user found to token
- **404 Not Found**:
    - User not found (target or sender)
- **409 Conflict**:
    - Friend request already exists
- **500 Internal Server Error**:
    - Unexpected error


## 🔹 PUT `/users/friendrequests/{friendRequestId}`

Retrieve all friend requests for the authenticated user.

- **URL:** `http://localhost:8080/users/friendRequests/1`
- **Method:** `PUT`
- **Headers:**
    - `Authorization: <token>`
- **Request-Body**:

```json
{
    "status": "ACCEPTED"
}
```
### ✅ Response

```json
{
    "id": 1,
    "sender": {
        "id": 1,
        "username": "luca2",
        "status": "ONLINE",
        "token": "174e773d-4185-467a-9402-8881d4990e4c",
        "bestGamePlayed": null,
        "friends": [],
        "inGame": false
    },
    "target": {
        "id": 2,
        "username": "luca",
        "status": "ONLINE",
        "token": "7d6d3edd-ecd3-4e1e-8378-1118a1463647",
        "bestGamePlayed": null,
        "friends": [],
        "inGame": false
    },
    "status": "ACCEPTED",
    "timeStamp": "2025-04-14T14:13:49.276957",
    "message": "hello world"
}
```
###  ❌ Error Responses
- **400 Bad Request**:
    - RequestId is null
    - Trying to change from ACCEPTED or DECLINED to Pending
- **401 Unauthorized**:
    - empty token or no user found to token 
    - user is not the sender or target of given friend request
- **404 Not Found**:
  - Friend request not found
- **500 Internal Server Error**:
    - Unexpected error
