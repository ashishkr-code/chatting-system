# Advanced Web Chatting Application (React + Java)

## Project Overview
This project is an advanced real-time chatting application designed for Web Browsers. We have pivoted from a JavaFX desktop app to a modern Web Application. It features text, voice, and video communication capabilities using WebSockets and WebRTC.

## Tech Stack
* **Frontend:** React.js, HTML/CSS, Native Browser APIs (WebSockets, WebRTC)
* **Backend:** Java (Core Java), Maven
* **Networking (Server):** Java WebSocket Server (`org.java-websocket` library) for real-time bi-directional communication with browsers.
* **Database:** MySQL (Hibernate ORM)
* **Multimedia:** WebRTC (for Peer-to-Peer Video/Audio calls directly in the browser)

## Key Features
1. **User Authentication:** Secure login and registration.
2. **Real-time Text Chat:** 1-on-1 messaging and Group chats (Global).
3. **Active Users Sync:** Real-time updates of who is online.
4. **Chat History:** Persistent storage of messages in a MySQL database.
5. **Friend Management:** Add, remove, and manage friends.
6. **Audio & Video Calling:** Browser-to-Browser WebRTC calling, with the Java server acting as a Signaling Server.

## Implementation Plan (Web Pivot)

### Phase 1: Backend WebSocket Server Setup
* Add the `Java-WebSocket` dependency to Maven.
* Convert our existing raw `ServerSocket` to a `WebSocketServer` so browsers can connect to it.
* Adjust the JSON `Packet` logic to seamlessly communicate with JavaScript clients.

### Phase 2: React Frontend & Text Chat
* Initialize a React project (using Vite).
* Build the Login & Registration pages.
* Build the Main Chat Dashboard UI (Sidebar for users, Chat area for messages).
* Connect React's native `WebSocket` API to our Java Backend (e.g., `ws://localhost:5000`).
* Test sending and receiving text messages over the web.

### Phase 3: Friend Management & Private Chat
* Update the database and backend logic to support Friendships (Requests, Accepted, Blocked).
* Add UI functionality in React to switch between Global Chat and Direct Messages.
* Route private messages through the Java WebSocket server directly to the intended receiver.

### Phase 4: WebRTC Signaling (Audio / Video Calls)
* Implement WebRTC on the React frontend.
* The React app will capture audio/video using `navigator.mediaDevices.getUserMedia()`.
* Update the Java Server to handle "SDP_OFFER", "SDP_ANSWER", and "ICE_CANDIDATE" packets (Signaling relay).
* Establish a high-performance Peer-to-Peer connection between two browsers for real-time video calling.

## Conclusion
Pivoting to the web makes the app much more accessible (no installation required for users). Using React gives us a modern UI, and WebRTC solves the extreme complexity of handling audio/video manually in Java, letting the browsers handle the heavy lifting.
