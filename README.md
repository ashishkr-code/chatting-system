# 💬 Nexus Web Chat — Full-Stack Real-Time Platform

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![React](https://img.shields.io/badge/React-19-cyan.svg)
![Vite](https://img.shields.io/badge/Vite-8.3-purple.svg)
![WebSockets](https://img.shields.io/badge/WebSockets-RFC6455-brightgreen.svg)
![WebRTC](https://img.shields.io/badge/WebRTC-Audio%20Calling-red.svg)
![Database](https://img.shields.io/badge/Database-TiDB%20%2F%20MySQL-blue.svg)
![Docker](https://img.shields.io/badge/Deployment-Docker%20%2B%20Render-blue.svg)

> **Nexus Web Chat** is a modern, responsive, real-time messaging and peer-to-peer audio calling web application. It features real-time 1-on-1 private messaging, global group rooms, photo/file sharing, WhatsApp-style read receipts (blue ticks), message reactions, threaded replies, contact blocking, a built-in time-pass minigame, and mobile-responsive layout.

---

## 🌟 Live Demo & Deployments

* 🌐 **Live Web Application (Frontend):** [https://chatting-system-1-nntp.onrender.com](https://chatting-system-1-nntp.onrender.com)
* ⚙️ **Live WebSocket Server (Backend):** `wss://chatting-system-1xf0.onrender.com`
* 🗄️ **Cloud Database:** TiDB Cloud Serverless (AWS Singapore Cluster)

---

## ✨ Key Features

| Category | Highlights |
| :--- | :--- |
| 🚀 **Real-Time Messaging** | High-performance full-duplex communication via Java-WebSocket. No polling. |
| 📞 **Live Voice Calling** | Peer-to-Peer encrypted audio calls via WebRTC with Google STUN integration. |
| 📱 **Mobile Responsive** | WhatsApp-style mobile navigation, back button (`←`), auto-zoom prevention, `100dvh` full-height fit. |
| 🖼️ **Media & File Sharing** | Send photos and files directly in chat with inline preview and one-click download. |
| ✔️ **Read Receipts** | WhatsApp-style status ticks: Single Grey (`✓` Sent), Double Grey (`✓✓` Delivered), Double Blue (`✓✓` Read). |
| 😃 **Reactions & Replies** | Double-click any message to react with emojis (`👍`, `❤️`, `😂`, `🔥`, `😢`) or reply with quoted snippets (`↩️`). |
| 🛡️ **User Security & Blocks** | Search users, send/accept friend requests, and block toxic users. Secure full logout button (`🚪 Logout`). |
| 🎮 **Time Pass Minigame** | Built-in Tic-Tac-Toe vs AI bot in chat header for passing time while waiting for replies. |
| 🎨 **Modern Glassmorphism UI** | Smooth keyframe gradient backgrounds, floating shapes, and 3D pop-in chat bubbles. |

---

## 🏗️ System Architecture

```
┌─────────────────────────────────┐
│     React 19 / Vite Frontend    │
│  (Desktop & Mobile Responsive)  │
└──────────────▲──────────────────┘
               │  WebSocket (JSON Packets)
               ▼
┌─────────────────────────────────┐       WebRTC P2P Audio
│    Java 21 WebSocket Server     │ ◄──────────────────────────► Remote Peer
│ (Render Docker Container :10000)│
└──────────────▲──────────────────┘
               │  Hibernate 6 ORM (SSL)
               ▼
┌─────────────────────────────────┐
│      TiDB Cloud Serverless      │
│  (MySQL Compatible Cloud DB)    │
└─────────────────────────────────┘
```

For complete architectural details, packet specifications, and class diagrams, refer to [PROJECT_DOCUMENTATION.md](PROJECT_DOCUMENTATION.md).

---

## 💻 Tech Stack

* **Frontend:** React 19, Vite, CSS3 (Glassmorphism, Keyframe Animations), WebRTC API, WebSocket API.
* **Backend:** Java 21, `org.java-websocket` (1.5.3), Google Gson (2.10.1), Maven, Maven Shade Plugin.
* **ORM & Database:** Hibernate ORM 6.4.4, MySQL Connector/J 8.3.0, TiDB Cloud Serverless.
* **DevOps:** Multi-Stage Dockerfile, Render Web Service & Static Site.

---

## 🚀 Local Development Setup

### 1. Prerequisites
* **Java Development Kit (JDK):** Version 21 or higher.
* **Node.js:** Version 18 or higher (with `npm`).
* **Maven:** Version 3.8 or higher.
* **MySQL:** Local MySQL instance or TiDB Cloud connection.

---

### 2. Configure Database
Update `src/main/resources/application.properties` with your database credentials:
```properties
db.url=jdbc:mysql://localhost:3306/chatapp?useSSL=false
db.username=root
db.password=your_password
hibernate.hbm2ddl.auto=update
hibernate.show_sql=true
```

---

### 3. Run the Backend Server
```bash
# Compile and start the Java WebSocket Server on port 5000
mvn clean compile exec:java@server
```
You will see:
```text
WebSocket Server is running on port 5000
WebSocket Server successfully started on port: 5000
```

---

### 4. Run the Frontend (React / Vite)
```bash
cd client-web
npm install
npm run dev
```
Open [http://localhost:5173](http://localhost:5173) in your browser.

---

## 🐳 Docker & Cloud Deployment

### Backend on Render (Web Service)
1. Repository contains a production-ready, multi-stage `Dockerfile`.
2. On Render Dashboard: Create **New Web Service** → Connect this repo.
3. Choose **Docker** as the Runtime.
4. Set Region to **Singapore** and Instance to **Free**.
5. Add **Environment Variables** (Security Best Practice):
   * `DB_URL`: `jdbc:mysql://<HOST>:<PORT>/<DATABASE>?sslMode=VERIFY_IDENTITY&useSSL=true`
   * `DB_USERNAME`: `<YOUR_USERNAME>`
   * `DB_PASSWORD`: `<YOUR_PASSWORD>`
6. Render will automatically build the Fat JAR and run the service.

### Frontend on Render (Static Site)
1. On Render Dashboard: Create **New Static Site** → Connect this repo.
2. Root Directory: `client-web`
3. Build Command: `npm run build`
4. Publish Directory: `dist`
5. Environment Variable:
   * `VITE_WS_URL`: `wss://your-backend-service.onrender.com`

---

## 📖 In-Depth Documentation

For complete technical specifications, see:
* [📘 Detailed Architecture & Engineering Documentation](PROJECT_DOCUMENTATION.md)

---

## 👤 Author
* **Ashish Kumar** — [GitHub Profile](https://github.com/ashishkr-code)
