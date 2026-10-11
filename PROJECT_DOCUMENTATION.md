# 📘 Nexus Chat Application — Complete Technical Documentation

> **A Full-Stack, Real-Time Messaging & Voice Communication Platform**  
> Built with Java 21, WebSockets, Hibernate ORM, MySQL (TiDB Cloud), React (Vite), WebRTC, and Docker.

---

## 📑 Table of Contents
1. [System Architecture Overview](#1-system-architecture-overview)
2. [Technology Stack](#2-technology-stack)
3. [Database Schema & Data Models](#3-database-schema--data-models)
4. [WebSocket Communication Protocol](#4-websocket-communication-protocol)
5. [WebRTC Peer-to-Peer Voice Calling Engine](#5-webrtc-peer-to-peer-voice-calling-engine)
6. [Frontend Architecture & Responsive Design](#6-frontend-architecture--responsive-design)
7. [Authentication, Security & Session Lifecycle](#7-authentication-security--session-lifecycle)
8. [Features Breakdown & User Workflows](#8-features-breakdown--user-workflows)
9. [Deployment & Infrastructure Guide](#9-deployment--infrastructure-guide)
10. [Troubleshooting & FAQ](#10-troubleshooting--faq)

---

## 1. System Architecture Overview

Nexus Chat follows a distributed client-server architecture with hybrid Peer-to-Peer (P2P) multimedia transmission.

```
┌──────────────────────────────────────────────────────────┐
│                   React Web Clients                      │
│      (Desktop / Tablet / Mobile Responsive Frontend)     │
└───────────────▲───────────────────────────▲──────────────┘
                │                           │
  WebSocket     │                           │   WebRTC P2P
  JSON Packets  │                           │   Direct Audio Stream
 (Signaling,    │                           │  (Microphone Data)
  Chat, State)  │                           │
                ▼                           ▼
┌───────────────────────────────┐     ┌─────────────────────┐
│     Java WebSocket Server     │     │     Remote Peer     │
│   (Port 5000 / Render 10000)  │     │   (Other Browser)   │
│  - Active Session Registry    │     └─────────────────────┘
│  - Message Routing Engine     │
│  - WebRTC Signaling Broker    │
└───────────────▲───────────────┘
                │
         Hibernate 6 ORM
         (JDBC / SSL TLS)
                │
                ▼
┌───────────────────────────────┐
│     TiDB Cloud Serverless     │
│   (MySQL-Compatible Cloud DB) │
│   - Users & Credentials       │
│   - Chat History (LONGTEXT)   │
│   - Friendships & Blocks      │
└───────────────────────────────┘
```

### Key Architectural Concepts:
1. **Stateful WebSocket Connection:** Clients maintain a persistent TCP socket connection with the Java server. There is zero polling overhead.
2. **Relay for Text, P2P for Voice:** Text messages, files, reactions, and friend requests are persisted in the database via the server. Audio packets never hit the server; the server only brokers the initial WebRTC connection handshake (SDP Offer/Answer & ICE Candidates).
3. **Decoupled Frontend:** The frontend is compiled into static HTML/CSS/JS and served independently via a CDN, communicating with the backend purely via secure WebSocket (`wss://`).

---

## 2. Technology Stack

### Backend
* **Language:** Java 21 (Eclipse Temurin JDK)
* **Build System:** Apache Maven 3.9+
* **WebSocket Library:** `org.java-websocket:Java-WebSocket:1.5.3` (RFC 6455 compliant)
* **ORM:** Hibernate ORM 6.4.4.Final
* **Database Driver:** `com.mysql:mysql-connector-j:8.3.0`
* **JSON Serialization:** Google Gson 2.10.1
* **Packaging:** `maven-shade-plugin` for standalone Uber/Fat JAR generation.

### Frontend
* **Core:** React 19 / Vite
* **Languages:** JavaScript (ESNext), HTML5, CSS3 with Glassmorphism & Custom Keyframe Animations
* **Browser APIs:** 
  * `WebSocket API` for real-time duplex data transfer.
  * `WebRTC API` (`RTCPeerConnection`) for peer-to-peer audio transmission.
  * `MediaDevices API` (`getUserMedia`) for microphone audio capture.
  * `FileReader API` for Base64 multimedia conversions.

### Database & Cloud Hosting
* **Database:** TiDB Cloud Serverless (AWS `ap-southeast-1` Singapore cluster, TLSv1.2/1.3 encrypted).
* **Backend Hosting:** Render.com Docker Container (Web Service).
* **Frontend Hosting:** Render.com Static Site (Vite Build output `dist/`).

---

## 3. Database Schema & Data Models

### 3.1 `users` Table
Stores registered user identities, password hashes, real-time presence indicators, and user avatars.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INT` | `AUTO_INCREMENT, PRIMARY KEY` | Unique ID of the user. |
| `username` | `VARCHAR(50)` | `NOT NULL, UNIQUE` | Unique handle used for login and mentions. |
| `password_hash` | `VARCHAR(255)` | `NOT NULL` | User password credential. |
| `full_name` | `VARCHAR(100)` | `NULLABLE` | Display name of the user. |
| `status` | `VARCHAR(20)` | `DEFAULT 'OFFLINE'` | Current connection state (`ONLINE`, `OFFLINE`). |
| `profile_pic` | `LONGTEXT` | `NULLABLE` | Base64 data URI of the user avatar image. |
| `last_seen` | `TIMESTAMP` | `NULLABLE` | Timestamp of the user's last disconnect event. |
| `created_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | Account creation timestamp. |

### 3.2 `messages` Table
Stores global and 1-on-1 private conversations, media payloads, delivery acknowledgments, and emoji reactions.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INT` | `AUTO_INCREMENT, PRIMARY KEY` | Unique message identifier. |
| `sender_id` | `INT` | `NOT NULL, FK -> users(id)` | Foreign key referencing sender. Cascades on delete. |
| `receiver_id` | `INT` | `NULLABLE, FK -> users(id)` | `NULL` for Global Chat broadcast; User ID for 1-on-1 private chat. |
| `content` | `LONGTEXT` | `NOT NULL` | Text message or formatted file payload `[FILE]:<name>:<base64>`. |
| `reactions` | `LONGTEXT` | `DEFAULT '{}'` | JSON dictionary mapping usernames to emojis (e.g., `{"ashish":"❤️"}`). |
| `reply_to_id` | `INT` | `NULLABLE` | ID of the parent message being quoted in reply. |
| `message_status` | `VARCHAR(20)` | `DEFAULT 'SENT'` | Delivery state: `'SENT'` (✓), `'DELIVERED'` (✓✓), `'READ'` (✓✓ blue). |
| `timestamp` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | Time message was registered by the server. |

### 3.3 `friendships` Table
Manages the social graph between users, including requests, acceptances, and blocks.

| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `INT` | `AUTO_INCREMENT, PRIMARY KEY` | Unique relationship ID. |
| `requester_id` | `INT` | `NOT NULL, FK -> users(id)` | User who initiated the request/block. |
| `receiver_id` | `INT` | `NOT NULL, FK -> users(id)` | User who received the request/block. |
| `status` | `VARCHAR(50)` | `NOT NULL` | Relationship state: `'PENDING'`, `'ACCEPTED'`, or `'BLOCKED'`. |

*Constraint:* `UNIQUE KEY uq_friendship (requester_id, receiver_id)` ensures only one relationship row exists per pair.

---

## 4. WebSocket Communication Protocol

All communication over the WebSocket channel occurs via JSON-serialized `Packet` objects.

```json
{
  "type": "TEXT",
  "sender": "ashish",
  "receiver": "john",
  "content": "Hello there!",
  "id": 104,
  "reactions": "{\"john\":\"👍\"}",
  "replyToId": 98,
  "messageStatus": "DELIVERED",
  "profilePic": "data:image/png;base64,...",
  "lastSeen": 1728641200000
}
```

### Complete Packet Type Directory:

| Packet Type | Direction | Content Payload | Description |
| :--- | :--- | :--- | :--- |
| `REGISTER` | Client → Server | `password:fullName` | Requests registration of sender username. |
| `REGISTER_RESPONSE`| Server → Client | `SUCCESS` / `FAIL` | Confirms or rejects registration. |
| `LOGIN` | Client → Server | `password` | Authenticates user credentials. |
| `LOGIN_RESPONSE` | Server → Client | `SUCCESS` / `FAIL` | Confirms authentication and initiates history push. |
| `BROADCAST` | Bi-directional | String / File | Global room message sent to all active connections. |
| `TEXT` | Bi-directional | String / File | Private 1-on-1 message routed only to recipient. |
| `TYPING` | Client → Server → Client | `""` | Broadcasts active typing indicator to chat partner. |
| `STOP_TYPING` | Client → Server → Client | `""` | Clears typing indicator. |
| `UPDATE_MESSAGE_STATUS`| Bi-directional | `READ` / `DELIVERED` | Updates receipt ticks in DB and syncs sender UI. |
| `REACT_MESSAGE` | Bi-directional | Emoji character | Adds or replaces user's emoji reaction on message ID. |
| `DELETE_MESSAGE` | Bi-directional | Message ID string | Deletes message from database and removes from UI. |
| `SEARCH_USERS` | Client → Server | Search query | Queries database for matching usernames. |
| `SEARCH_RESULTS` | Server → Client | JSON Array of names | Returns search matches. |
| `SEND_REQUEST` | Client → Server | Target username | Creates `PENDING` friendship row. |
| `ACCEPT_REQUEST` | Client → Server | Requester username | Changes friendship status to `ACCEPTED`. |
| `BLOCK_USER` | Client → Server | Target username | Sets relationship to `BLOCKED`. |
| `UNBLOCK_USER` | Client → Server | Target username | Removes `BLOCKED` status. |
| `UPDATE_PROFILE_PIC` | Client → Server → All | Base64 image | Updates avatar in DB and notifies contacts. |
| `GET_USER_INFO` | Client → Server | Target username | Requests profile pic and last seen timestamp. |
| `USER_INFO` | Server → Client | Target username | Returns profile pic and last seen timestamp. |
| `CALL_OFFER` | Client → Server → Client | SDP Offer JSON | Initiates WebRTC voice call signaling. |
| `CALL_ANSWER` | Client → Server → Client | SDP Answer JSON | Accepts WebRTC voice call. |
| `ICE_CANDIDATE` | Client → Server → Client | Candidate JSON | Exchanges network routing candidate info. |

---

## 5. WebRTC Peer-to-Peer Voice Calling Engine

Nexus Chat provides real-time, low-latency audio calling directly between client browsers without routing media streams through the server.

### Signaling Workflow Sequence:

```
Caller (Browser A)              Java Server (Signaling)              Callee (Browser B)
       │                                   │                                  │
 1. Clicks 📞 Call                         │                                  │
 2. getUserMedia({audio: true})            │                                  │
 3. Creates RTCPeerConnection              │                                  │
 4. Creates SDP Offer                      │                                  │
 5. Sends CALL_OFFER ─────────────────────►│                                  │
       │                              Relay│───── CALL_OFFER ────────────────►│
       │                                   │                           6. Incoming Call Banner
       │                                   │                           7. Clicks Accept
       │                                   │                           8. getUserMedia({audio: true})
       │                                   │                           9. Sets Remote Description
       │                                   │                          10. Creates SDP Answer
       │                                   │◄──── Sends CALL_ANSWER ──────────│
       │◄───── Relay CALL_ANSWER ──────────│                                  │
11. Sets Remote Description                │                                  │
       │                                   │                                  │
12. ICE Candidates ───────────────────────►│                                  │
       │                              Relay│───── ICE Candidates ────────────►│
       │◄───── Relay ICE Candidates ───────│◄──── ICE Candidates ─────────────│
       │                                   │                                  │
       ▼══════════════════════════════════════════════════════════════════════▼
                         Direct Encrypted P2P Audio Stream
                                (WebRTC SRTP Channel)
```

### STUN Configuration:
The application uses Google's public STUN infrastructure (`stun:stun.l.google.com:19302`) to discover external IP addresses and navigate NAT routers without requiring expensive TURN servers.

---

## 6. Frontend Architecture & Responsive Design

### 6.1 Responsive Mobile / Desktop Layout
* **Desktop View (`> 768px`):** Split-screen layout mirroring WhatsApp Web. Left column contains user profile, global room link, user search, pending requests, blocked list, and active friends list. Right column contains the active chat room or Tic-Tac-Toe minigame.
* **Mobile View (`≤ 768px`):** 
  * Implements dynamic view toggling via `.show-sidebar` and `.show-chat` CSS classes.
  * When opening the app, users see the full-width chat and friend selection menu.
  * Tapping on any conversation smoothly slides into the full-width chat window.
  * Chat header provides a native `← Back` button to instantly return to the contact list.
  * `100dvh` viewport units prevent mobile browser URL bars from hiding the bottom message input.
  * Input field enforces `font-size: 16px` to prevent iOS Safari and Android Chrome from triggering undesirable layout auto-zooming.

### 6.2 UI Animations & Glassmorphism
* **Authentication Screen:** Animated 4-corner CSS gradient background with dynamic floating spherical particles rendered via CSS keyframes (`@keyframes floatUp`).
* **Chat Bubbles:** Spring pop-in animations (`@keyframes popInMsg`) on incoming messages, hover scale transitions (`transform: scale(1.02)`), and custom reaction badges.
* **Minigame (Tic-Tac-Toe vs Bot):** Fully interactive client-side game engine allowing users to play against an AI opponent while waiting for replies.

---

## 7. Authentication, Security & Session Lifecycle

1. **Clean Logout Flow:** 
   * Clicking `🚪 Logout` terminates active WebRTC voice calls, disposes of local media streams, closes the WebSocket connection, resets React state, and routes to the login tab.
   * On disconnect, the Java server handles `onClose()`, removing the user from the active socket map, broadcasting an updated online user list, and updating the database `last_seen` timestamp.
2. **Database Security:** All database queries utilize Hibernate `Query<T>` with named parameters (`:username`), eliminating SQL injection vulnerabilities.
3. **Encrypted Transport:**
   * Frontend: Served over HTTPS with TLS certificates.
   * WebSockets: Secured via `wss://` protocol.
   * Database: Communicates with TiDB Cloud using `sslMode=VERIFY_IDENTITY&useSSL=true`.

---

## 8. Features Breakdown & User Workflows

### 8.1 Multimedia & File Sharing
Users can upload photos and files up to the maximum browser memory limit via the `📎` button.
Files are encoded to Base64 data URLs on the client, transmitted via the WebSocket connection as `[FILE]:<filename>:<base64>`, stored in the MySQL `LONGTEXT` column, and rendered as inline images or downloadable file cards on the recipient's screen.

### 8.2 Message Delivery Status (Ticks)
* **Single Grey Tick (`✓`):** Message saved in database; recipient is offline (`SENT`).
* **Double Grey Ticks (`✓✓`):** Recipient received message over active WebSocket (`DELIVERED`).
* **Double Blue Ticks (`✓✓`):** Recipient has the active chat window open (`READ`).

### 8.3 Message Reactions & Replies
* **Reactions:** Double-clicking any message opens a floating emoji picker (`👍`, `❤️`, `😂`, `🔥`, `😢`, `↩️`).
* **Replies:** Selecting the `↩️` action attaches the quoted message to the input banner. The parent message content and author are rendered directly above the reply.

### 8.4 User Management & Blocking
* **Search Users:** Real-time search across the database.
* **Add Friend:** Inline `➕` button directly from Global chat or search bar.
* **Block User:** Clicking `🚫` marks the user as blocked, immediately removing their messages from the screen and preventing any incoming private messages or calls.
* **Unblock User:** Managed through the dedicated `Blocked Users` section in the sidebar.

---

## 9. Deployment & Infrastructure Guide

### 9.1 Database Deployment (TiDB Cloud)
1. Create a free cluster on [TiDB Cloud](https://tidbcloud.com/).
2. Open the SQL Editor and execute the DDL queries for `users`, `messages`, and `friendships`.
3. Configure `src/main/resources/application.properties` with the provided Host, Port 4000, Username, and Password.

### 9.2 Backend Deployment (Render Web Service)
1. Repository includes a multi-stage `Dockerfile`.
2. On Render Dashboard: **New +** → **Web Service**.
3. Select `ashishkr-code/chatting-system` repository.
4. Runtime: **Docker**.
5. Instance Type: **Free**.
6. **Environment Variables** (Security Best Practice):
   * `DB_URL`: `jdbc:mysql://<HOST>:<PORT>/<DATABASE>?sslMode=VERIFY_IDENTITY&useSSL=true`
   * `DB_USERNAME`: `<YOUR_DB_USERNAME>`
   * `DB_PASSWORD`: `<YOUR_DB_PASSWORD>`
7. The service starts on port 10000 and exposes public URL `https://<service-name>.onrender.com`.

### 9.3 Frontend Deployment (Render Static Site)
1. On Render Dashboard: **New +** → **Static Site**.
2. Select the repository.
3. Root Directory: `client-web`
4. Build Command: `npm run build`
5. Publish Directory: `dist`
6. Environment Variables:
   * Key: `VITE_WS_URL`
   * Value: `wss://<backend-service-name>.onrender.com`

---

## 10. Troubleshooting & FAQ

#### Q: Why does opening the backend URL in Chrome show `404 WebSocket Upgrade Failure`?
**A:** The backend is a pure WebSocket server, not an HTTP web server. Browsers sending standard HTTP GET requests receive a 404 because no webpage exists there. The frontend must connect to it using `wss://`.

#### Q: Why was the frontend screen initially blank on deployment?
**A:** Render's Static Site publish directory was serving unbuilt source files instead of the compiled `dist/` directory. Setting Publish Directory to `dist` resolves the issue.

#### Q: How does voice calling work through strict corporate firewalls?
**A:** The app leverages Google STUN servers. In extreme symmetric NAT environments, a dedicated TURN relay server would be required, but standard residential and mobile networks connect seamlessly via STUN.
