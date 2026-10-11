import { useState, useEffect, useRef } from 'react'
import './App.css'

function App() {
  const [isLoginTab, setIsLoginTab] = useState(true)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [fullName, setFullName] = useState('')
  
  const [isLoggedIn, setIsLoggedIn] = useState(false)
  const [errorMsg, setErrorMsg] = useState('')
  
  const [activeUsers, setActiveUsers] = useState([])
  const [friends, setFriends] = useState([])
  const [pendingRequests, setPendingRequests] = useState([])
  const [blockedUsers, setBlockedUsers] = useState([])
  
  const [userProfiles, setUserProfiles] = useState({})
  const [searchQuery, setSearchQuery] = useState('')
  const [searchResults, setSearchResults] = useState([])
  
  const [currentMessage, setCurrentMessage] = useState('')
  const [selectedChat, setSelectedChat] = useState('GLOBAL') 
  const [replyingTo, setReplyingTo] = useState(null)
  
  const [globalMessages, setGlobalMessages] = useState([])
  const [privateMessages, setPrivateMessages] = useState({}) 
  
  const [typingUsers, setTypingUsers] = useState({})
  const [activeReactionMsg, setActiveReactionMsg] = useState(null)

  // Mini-Game State (Tic Tac Toe)
  const [isGameOpen, setIsGameOpen] = useState(false)
  const [board, setBoard] = useState(Array(9).fill(null))
  const [xIsNext, setXIsNext] = useState(true)
  const [winner, setWinner] = useState(null)
  
  const ws = useRef(null)
  const messagesEndRef = useRef(null)
  const fileInputRef = useRef(null)
  const profilePicRef = useRef(null)
  const typingTimeoutRef = useRef(null)

  // WebRTC
  const [isInCall, setIsInCall] = useState(false)
  const [incomingCall, setIncomingCall] = useState(null)
  const [remoteStream, setRemoteStream] = useState(null)
  const peerConnection = useRef(null)
  const localStream = useRef(null)
  const remoteAudioRef = useRef(null)

  useEffect(() => {
    if (!isGameOpen) messagesEndRef.current?.scrollIntoView({ behavior: "smooth" })
  }, [globalMessages, privateMessages, selectedChat, isGameOpen])

  useEffect(() => {
    if (remoteAudioRef.current && remoteStream) remoteAudioRef.current.srcObject = remoteStream;
  }, [remoteStream])
  
  useEffect(() => {
    if (selectedChat !== 'GLOBAL' && ws.current) {
      ws.current.send(JSON.stringify({ type: 'GET_USER_INFO', sender: username, receiver: 'SERVER', content: selectedChat }))
      setIsGameOpen(false) // Close game when switching chat
      
      setPrivateMessages(prev => {
        const chat = prev[selectedChat] || []
        let changed = false;
        const newChat = chat.map(m => {
          if (m.sender === selectedChat && m.messageStatus !== 'READ') {
            changed = true;
            ws.current.send(JSON.stringify({ type: 'UPDATE_MESSAGE_STATUS', sender: username, receiver: m.sender, content: 'READ', id: m.id }))
            return { ...m, messageStatus: 'READ' }
          }
          return m
        })
        return changed ? { ...prev, [selectedChat]: newChat } : prev
      })
    } else {
      setIsGameOpen(false)
    }
  }, [selectedChat])

  // Bot Logic for Tic Tac Toe
  useEffect(() => {
    if (isGameOpen && !xIsNext && !winner) {
      const timer = setTimeout(() => {
        const emptyIndices = board.map((v, i) => v === null ? i : null).filter(v => v !== null)
        if (emptyIndices.length > 0) {
          const randomIdx = emptyIndices[Math.floor(Math.random() * emptyIndices.length)]
          handleGameClick(randomIdx, false)
        }
      }, 600)
      return () => clearTimeout(timer)
    }
  }, [xIsNext, isGameOpen, winner])

  const connectWebSocket = () => {
    ws.current = new WebSocket('ws://localhost:5000')

    ws.current.onopen = () => {
      if (isLoginTab) ws.current.send(JSON.stringify({ type: 'LOGIN', sender: username, receiver: 'SERVER', content: password }))
      else ws.current.send(JSON.stringify({ type: 'REGISTER', sender: username, receiver: 'SERVER', content: `${password}:${fullName}` }))
    }

    ws.current.onmessage = async (event) => {
      const packet = JSON.parse(event.data)
      
      if (packet.type === 'LOGIN_RESPONSE') {
        if (packet.content === 'SUCCESS') setIsLoggedIn(true)
        else { setErrorMsg('Invalid credentials'); ws.current.close() }
      } else if (packet.type === 'REGISTER_RESPONSE') {
        if (packet.content === 'SUCCESS') { setErrorMsg('Registration successful! Please login.'); setIsLoginTab(true); ws.current.close() }
        else { setErrorMsg('Registration failed.'); ws.current.close() }
      } else if (packet.type === 'USER_INFO') {
        setUserProfiles(prev => ({ ...prev, [packet.content]: { profilePic: packet.profilePic, lastSeen: packet.lastSeen } }))
      } else if (packet.type === 'UPDATE_PROFILE_PIC') {
        setUserProfiles(prev => ({ ...prev, [packet.sender]: { ...prev[packet.sender], profilePic: packet.content } }))
      } else if (packet.type === 'FRIENDS_LIST') {
        setFriends(JSON.parse(packet.content))
        JSON.parse(packet.content).forEach(f => ws.current.send(JSON.stringify({ type: 'GET_USER_INFO', sender: username, receiver: 'SERVER', content: f })))
      } else if (packet.type === 'PENDING_REQUESTS') {
        setPendingRequests(JSON.parse(packet.content))
      } else if (packet.type === 'BLOCKED_USERS') {
        setBlockedUsers(JSON.parse(packet.content))
      } else if (packet.type === 'SEARCH_RESULTS') {
        setSearchResults(JSON.parse(packet.content))
      } else if (packet.type === 'NEW_REQUEST' || packet.type === 'REQUEST_ACCEPTED' || packet.type === 'UNBLOCK_USER') {
        ws.current.send(JSON.stringify({ type: 'GET_FRIENDS', sender: username, receiver: 'SERVER', content: '' }))
      } else if (packet.type === 'HISTORY_GLOBAL') {
        setGlobalMessages(JSON.parse(packet.content))
      } else if (packet.type === 'HISTORY_PRIVATE') {
        const history = JSON.parse(packet.content)
        if (history.length > 0) {
          const peer = history[0].sender === username ? history[0].receiver : history[0].sender
          setPrivateMessages(prev => ({ ...prev, [peer]: history }))
          history.forEach(m => {
            if (m.sender !== username && m.messageStatus !== 'READ' && selectedChat === m.sender) {
              ws.current.send(JSON.stringify({ type: 'UPDATE_MESSAGE_STATUS', sender: username, receiver: m.sender, content: 'READ', id: m.id }))
            }
          })
        }
      } else if (packet.type === 'USER_LIST') {
        setActiveUsers(JSON.parse(packet.content))
      } else if (packet.type === 'BROADCAST') {
        setGlobalMessages(prev => {
          if (!prev.find(m => m.id === packet.id)) return [...prev, packet]
          return prev
        })
      } else if (packet.type === 'TEXT') {
        const peer = packet.sender === username ? packet.receiver : packet.sender
        if (selectedChat === peer && packet.sender !== username) {
          packet.messageStatus = 'READ'
          ws.current.send(JSON.stringify({ type: 'UPDATE_MESSAGE_STATUS', sender: username, receiver: peer, content: 'READ', id: packet.id }))
        }
        setPrivateMessages(prev => {
          const chat = prev[peer] || []
          if (!chat.find(m => m.id === packet.id)) return { ...prev, [peer]: [...chat, packet] }
          return prev
        })
      } else if (packet.type === 'UPDATE_MESSAGE_STATUS') {
        const peer = packet.sender === username ? packet.receiver : packet.sender
        setPrivateMessages(prev => {
          if (!prev[peer]) return prev
          return { ...prev, [peer]: prev[peer].map(m => m.id === packet.id ? { ...m, messageStatus: packet.content } : m) }
        })
      } else if (packet.type === 'TYPING') {
        setTypingUsers(prev => ({ ...prev, [packet.sender]: true }))
      } else if (packet.type === 'STOP_TYPING') {
        setTypingUsers(prev => ({ ...prev, [packet.sender]: false }))
      } else if (packet.type === 'DELETE_MESSAGE') {
        const idToDelete = parseInt(packet.content)
        setGlobalMessages(prev => prev.filter(m => m.id !== idToDelete))
        const peer = packet.sender === username ? packet.receiver : packet.sender
        setPrivateMessages(prev => {
          if (!prev[peer]) return prev
          return { ...prev, [peer]: prev[peer].filter(m => m.id !== idToDelete) }
        })
      } else if (packet.type === 'REACT_MESSAGE') {
        const updateReactions = (msgs) => msgs.map(m => {
          if (m.id === packet.id) {
            const rx = m.reactions ? JSON.parse(m.reactions) : {}
            rx[packet.sender] = packet.content
            return { ...m, reactions: JSON.stringify(rx) }
          }
          return m
        })
        setGlobalMessages(updateReactions)
        const peer = packet.sender === username ? packet.receiver : packet.sender
        setPrivateMessages(prev => {
          if (!prev[peer]) return prev
          return { ...prev, [peer]: updateReactions(prev[peer]) }
        })
      } 
      else if (packet.type === 'CALL_OFFER') setIncomingCall({ from: packet.sender, offer: JSON.parse(packet.content) })
      else if (packet.type === 'CALL_ANSWER') peerConnection.current?.setRemoteDescription(new RTCSessionDescription(JSON.parse(packet.content)))
      else if (packet.type === 'ICE_CANDIDATE') peerConnection.current?.addIceCandidate(new RTCIceCandidate(JSON.parse(packet.content))).catch(e=>console.error(e))
    }
  }

  const handleAuth = (e) => {
    e.preventDefault()
    if (!username.trim() || !password.trim()) return
    connectWebSocket()
  }

  const handleSearch = (e) => {
    e.preventDefault()
    if (!searchQuery.trim() || !ws.current) return
    ws.current.send(JSON.stringify({ type: 'SEARCH_USERS', sender: username, receiver: 'SERVER', content: searchQuery }))
  }

  const sendFriendRequest = (user) => { if(ws.current) ws.current.send(JSON.stringify({ type: 'SEND_REQUEST', sender: username, receiver: 'SERVER', content: user })) }
  const acceptRequest = (user) => { if(ws.current) ws.current.send(JSON.stringify({ type: 'ACCEPT_REQUEST', sender: username, receiver: 'SERVER', content: user })) }
  const blockUser = (user) => { if(ws.current) { ws.current.send(JSON.stringify({ type: 'BLOCK_USER', sender: username, receiver: 'SERVER', content: user })); if(selectedChat===user) setSelectedChat('GLOBAL') } }
  const unblockUser = (user) => { if(ws.current) ws.current.send(JSON.stringify({ type: 'UNBLOCK_USER', sender: username, receiver: 'SERVER', content: user })) }

  const selectUser = (user) => {
    if (user === username) return
    setSelectedChat(user)
    if (ws.current) ws.current.send(JSON.stringify({ type: 'GET_PRIVATE_HISTORY', sender: username, receiver: 'SERVER', content: user }))
  }

  const sendMessage = (e) => {
    e.preventDefault()
    if (!currentMessage.trim() || !ws.current) return

    const packet = { 
      type: selectedChat === 'GLOBAL' ? 'BROADCAST' : 'TEXT', 
      sender: username, 
      receiver: selectedChat === 'GLOBAL' ? 'ALL' : selectedChat, 
      content: currentMessage,
      replyToId: replyingTo ? replyingTo.id : null
    }
    ws.current.send(JSON.stringify(packet))
    setCurrentMessage('')
    setReplyingTo(null)
    if (selectedChat !== 'GLOBAL') {
      ws.current.send(JSON.stringify({ type: 'STOP_TYPING', sender: username, receiver: selectedChat, content: '' }))
    }
  }

  const handleTyping = (e) => {
    setCurrentMessage(e.target.value)
    if (selectedChat !== 'GLOBAL' && ws.current) {
      ws.current.send(JSON.stringify({ type: 'TYPING', sender: username, receiver: selectedChat, content: '' }))
      clearTimeout(typingTimeoutRef.current)
      typingTimeoutRef.current = setTimeout(() => ws.current.send(JSON.stringify({ type: 'STOP_TYPING', sender: username, receiver: selectedChat, content: '' })), 2000)
    }
  }

  const handleReaction = (msgId, emoji) => {
    if (!ws.current) return
    ws.current.send(JSON.stringify({ type: 'REACT_MESSAGE', sender: username, receiver: selectedChat === 'GLOBAL' ? 'ALL' : selectedChat, content: emoji, id: msgId }))
    setActiveReactionMsg(null)
  }

  const handleProfilePicSelect = (e) => {
    const file = e.target.files[0]
    if (!file || !ws.current) return
    const reader = new FileReader()
    reader.onload = (event) => {
      const base64Data = event.target.result
      ws.current.send(JSON.stringify({ type: 'UPDATE_PROFILE_PIC', sender: username, receiver: 'ALL', content: base64Data }))
      setUserProfiles(prev => ({ ...prev, [username]: { ...prev[username], profilePic: base64Data } }))
    }
    reader.readAsDataURL(file)
  }

  const handleFileSelect = (e) => {
    const file = e.target.files[0]
    if (!file || !ws.current) return
    const reader = new FileReader()
    reader.onload = (event) => {
      const base64Data = event.target.result
      const packet = { type: selectedChat === 'GLOBAL' ? 'BROADCAST' : 'TEXT', sender: username, receiver: selectedChat === 'GLOBAL' ? 'ALL' : selectedChat, content: `[FILE]:${file.name}:${base64Data}`, replyToId: replyingTo ? replyingTo.id : null }
      ws.current.send(JSON.stringify(packet))
      setReplyingTo(null)
    }
    reader.readAsDataURL(file)
    e.target.value = null
  }

  // --- Tic Tac Toe Logic ---
  const calculateWinner = (squares) => {
    const lines = [[0,1,2],[3,4,5],[6,7,8],[0,3,6],[1,4,7],[2,5,8],[0,4,8],[2,4,6]]
    for (let i = 0; i < lines.length; i++) {
      const [a, b, c] = lines[i]
      if (squares[a] && squares[a] === squares[b] && squares[a] === squares[c]) return squares[a]
    }
    if (!squares.includes(null)) return 'Draw'
    return null
  }
  const handleGameClick = (i, isPlayer = true) => {
    if (board[i] || winner || (!xIsNext && isPlayer)) return
    const newBoard = [...board]
    newBoard[i] = xIsNext ? 'X' : 'O'
    setBoard(newBoard)
    setXIsNext(!xIsNext)
    setWinner(calculateWinner(newBoard))
  }
  const resetGame = () => { setBoard(Array(9).fill(null)); setXIsNext(true); setWinner(null); }

  // --- WebRTC ---
  const initPeerConnection = (targetUser) => {
    peerConnection.current = new RTCPeerConnection({ iceServers: [{ urls: 'stun:stun.l.google.com:19302' }] })
    localStream.current.getTracks().forEach(track => peerConnection.current.addTrack(track, localStream.current))
    peerConnection.current.onicecandidate = (e) => { if(e.candidate && ws.current) ws.current.send(JSON.stringify({ type: 'ICE_CANDIDATE', sender: username, receiver: targetUser, content: JSON.stringify(e.candidate) })) }
    peerConnection.current.ontrack = (e) => setRemoteStream(e.streams[0])
  }
  const startVoiceCall = async () => {
    try {
      localStream.current = await navigator.mediaDevices.getUserMedia({ audio: true })
      setIsInCall(true)
      initPeerConnection(selectedChat)
      const offer = await peerConnection.current.createOffer()
      await peerConnection.current.setLocalDescription(offer)
      ws.current.send(JSON.stringify({ type: 'CALL_OFFER', sender: username, receiver: selectedChat, content: JSON.stringify(offer) }))
    } catch (e) { alert("Microphone error: " + e.message) }
  }
  const acceptCall = async () => {
    if (!incomingCall) return
    try {
      localStream.current = await navigator.mediaDevices.getUserMedia({ audio: true })
      setIsInCall(true)
      initPeerConnection(incomingCall.from)
      await peerConnection.current.setRemoteDescription(new RTCSessionDescription(incomingCall.offer))
      const answer = await peerConnection.current.createAnswer()
      await peerConnection.current.setLocalDescription(answer)
      ws.current.send(JSON.stringify({ type: 'CALL_ANSWER', sender: username, receiver: incomingCall.from, content: JSON.stringify(answer) }))
      setIncomingCall(null)
    } catch (e) { alert("Microphone error: " + e.message) }
  }
  const endCall = () => {
    peerConnection.current?.close()
    peerConnection.current = null
    localStream.current?.getTracks().forEach(t => t.stop())
    localStream.current = null
    setRemoteStream(null)
    setIsInCall(false)
    setIncomingCall(null)
  }

  // --- Render Helpers ---
  const renderMessageContent = (content) => {
    if (content.startsWith('[FILE]:')) {
      const parts = content.split(':')
      const fileName = parts[1]
      const base64 = parts.slice(2).join(':')
      if (base64.startsWith('data:image')) return <img src={base64} alt={fileName} style={{ maxWidth: '100%', borderRadius: '8px' }} />
      return <a href={base64} download={fileName} className="download-link">📁 Download {fileName}</a>
    }
    return <div className="message-text">{content}</div>
  }
  const formatLastSeen = (timestamp) => {
    if (!timestamp) return 'Last seen recently'
    const d = new Date(timestamp)
    return `Last seen ${d.toLocaleDateString()} at ${d.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}`
  }
  const getStatusIcon = (status) => {
    if (status === 'SENT') return <span style={{color: '#9ca3af', fontSize:'12px', marginLeft:'5px'}}>✓</span>
    if (status === 'DELIVERED') return <span style={{color: '#9ca3af', fontSize:'12px', marginLeft:'5px'}}>✓✓</span>
    if (status === 'READ') return <span style={{color: '#3b82f6', fontSize:'12px', marginLeft:'5px'}}>✓✓</span>
    return null
  }

  if (!isLoggedIn) {
    return (
      <div className="auth-container">
        <div className="floating-shapes">
          <div className="shape"></div>
          <div className="shape"></div>
          <div className="shape"></div>
          <div className="shape"></div>
        </div>
        <div className="auth-box">
          <h2>Nexus Web Chat</h2>
          <div className="tabs">
            <button className={isLoginTab ? 'active' : ''} onClick={() => setIsLoginTab(true)}>Login</button>
            <button className={!isLoginTab ? 'active' : ''} onClick={() => setIsLoginTab(false)}>Register</button>
          </div>
          <form onSubmit={handleAuth}>
            {!isLoginTab && <input type="text" placeholder="Full Name" value={fullName} onChange={(e) => setFullName(e.target.value)} />}
            <input type="text" placeholder="Username" value={username} onChange={(e) => setUsername(e.target.value)} />
            <input type="password" placeholder="Password" value={password} onChange={(e) => setPassword(e.target.value)} />
            <button type="submit">{isLoginTab ? 'Login' : 'Create Account'}</button>
          </form>
          {errorMsg && <p className="error" style={{marginTop: '15px'}}>{errorMsg}</p>}
        </div>
      </div>
    )
  }

  let rawMessages = selectedChat === 'GLOBAL' ? globalMessages : (privateMessages[selectedChat] || [])
  const currentChatMessages = rawMessages.filter(msg => !blockedUsers.includes(msg.sender))
  const isTyping = selectedChat !== 'GLOBAL' && typingUsers[selectedChat]
  const targetInfo = selectedChat !== 'GLOBAL' ? userProfiles[selectedChat] : null

  return (
    <div className="app-wrapper">
      <div className="chat-container">
        <div className="sidebar">
          <div className="sidebar-header" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <div className="profile-pic-container" onClick={() => profilePicRef.current.click()}>
              <img src={userProfiles[username]?.profilePic || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png'} alt="Profile" className="profile-pic" />
              <input type="file" ref={profilePicRef} style={{ display: 'none' }} accept="image/*" onChange={handleProfilePicSelect} />
            </div>
            <h3>{username}</h3>
          </div>
          <div className="chat-menu">
            <div className={`menu-item ${selectedChat === 'GLOBAL' ? 'selected' : ''}`} onClick={() => setSelectedChat('GLOBAL')}>
              🌍 Global Chat
            </div>
            
            <form onSubmit={handleSearch} className="search-bar">
              <input type="text" placeholder="Search Users..." value={searchQuery} onChange={(e) => setSearchQuery(e.target.value)} />
              <button type="submit">🔍</button>
            </form>
            
            {searchResults.length > 0 && (
              <div className="search-results">
                <h4>Search Results</h4>
                {searchResults.map((u, i) => (
                  <div key={i} className="search-item">
                    <span>{u}</span>
                    <button onClick={() => sendFriendRequest(u)}>Add</button>
                  </div>
                ))}
              </div>
            )}

            {pendingRequests.length > 0 && (
              <div className="pending-requests">
                <h4>Pending Requests</h4>
                {pendingRequests.map((u, i) => (
                  <div key={i} className="pending-item">
                    <span>{u}</span>
                    <button onClick={() => acceptRequest(u)}>Accept</button>
                  </div>
                ))}
              </div>
            )}

            {blockedUsers.length > 0 && (
              <div className="pending-requests" style={{ background: '#fee2e2', borderColor: '#fca5a5' }}>
                <h4 style={{ color: '#991b1b' }}>Blocked Users</h4>
                {blockedUsers.map((u, i) => (
                  <div key={i} className="pending-item">
                    <span>{u}</span>
                    <button style={{ background: '#ef4444' }} onClick={() => unblockUser(u)}>Unblock</button>
                  </div>
                ))}
              </div>
            )}

            <h4>Friends</h4>
            {friends.map((user, index) => {
              const isOnline = activeUsers.includes(user)
              const pic = userProfiles[user]?.profilePic || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png'
              return (
                <div key={index} className={`menu-item ${selectedChat === user ? 'selected' : ''}`} onClick={() => selectUser(user)} style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <img src={pic} alt="dp" style={{ width: '35px', height: '35px', borderRadius: '50%', objectFit: 'cover' }} />
                  <div>
                    <div style={{fontWeight: 'bold', fontSize: '15px'}}>{user}</div>
                    <div style={{ fontSize: '11px', color: isOnline ? '#10b981' : '#6b7280' }}>{isOnline ? '🟢 Online' : '⚪ Offline'}</div>
                  </div>
                </div>
              )
            })}
          </div>
        </div>
        
        <div className="main-chat">
          <div className="chat-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '15px' }}>
              {selectedChat !== 'GLOBAL' && targetInfo && (
                <img src={targetInfo.profilePic || 'https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_1280.png'} alt="dp" style={{ width: '45px', height: '45px', borderRadius: '50%', objectFit: 'cover', border: '2px solid #667eea' }} />
              )}
              <div>
                <h2 style={{ margin: 0, color: '#333' }}>{selectedChat === 'GLOBAL' ? '🌍 Global Chat' : selectedChat}</h2>
                {isTyping ? (
                  <span className="typing-indicator" style={{ fontSize: '13px' }}>Typing...</span>
                ) : (
                  selectedChat !== 'GLOBAL' && (
                    <span style={{ fontSize: '13px', color: '#6b7280' }}>
                      {activeUsers.includes(selectedChat) ? 'Online' : formatLastSeen(targetInfo?.lastSeen)}
                    </span>
                  )
                )}
              </div>
            </div>
            
            <div className="header-actions">
              <button className="play-game-btn" onClick={() => setIsGameOpen(!isGameOpen)}>
                {isGameOpen ? '❌ Close Game' : '🎮 Time Pass'}
              </button>
              {selectedChat !== 'GLOBAL' && !isInCall && (
                <button className="call-btn" onClick={startVoiceCall} title="Voice Call">📞</button>
              )}
              {isInCall && (
                <button className="end-call-btn" onClick={endCall} title="End Call">🛑 End Call</button>
              )}
            </div>
          </div>
          
          {incomingCall && (
            <div className="incoming-call-banner">
              <p>📞 Incoming Voice Call from <strong>{incomingCall.from}</strong></p>
              <div>
                <button className="accept" onClick={acceptCall}>Accept</button>
                <button className="reject" onClick={() => setIncomingCall(null)}>Reject</button>
              </div>
            </div>
          )}

          <audio ref={remoteAudioRef} autoPlay />
          
          <div className="messages-area">
            {isGameOpen ? (
              <div className="game-container">
                <h2>Tic Tac Toe vs Bot 🤖</h2>
                <div className="tic-tac-toe-board">
                  {board.map((val, i) => (
                    <div key={i} className="tic-tac-toe-cell" onClick={() => handleGameClick(i)}>
                      <span style={{ color: val === 'X' ? '#667eea' : '#ef4444' }}>{val}</span>
                    </div>
                  ))}
                </div>
                {winner ? (
                  <div style={{ textAlign: 'center' }}>
                    <h3 style={{ color: winner === 'X' ? '#10b981' : (winner === 'Draw' ? '#6b7280' : '#ef4444') }}>
                      {winner === 'X' ? '🎉 You Won!' : (winner === 'Draw' ? '🤝 Draw!' : '🤖 Bot Won!')}
                    </h3>
                    <button className="play-game-btn" onClick={resetGame}>Play Again</button>
                  </div>
                ) : (
                  <p>{xIsNext ? "Your Turn (X)" : "Bot is thinking..."}</p>
                )}
              </div>
            ) : currentChatMessages.length === 0 ? (
              <div style={{ margin: 'auto', textAlign: 'center', color: '#9ca3af', opacity: 0.7 }}>
                <div style={{ fontSize: '50px', marginBottom: '10px' }}>💬</div>
                <p>No messages yet. Send a message to start!</p>
                <p>Bored? Click "Time Pass" above to play a game!</p>
              </div>
            ) : (
              currentChatMessages.map((msg, index) => {
                const isMe = msg.sender === username
                const reactionsObj = msg.reactions && msg.reactions !== "{}" ? JSON.parse(msg.reactions) : null
                const repliedToMsg = msg.replyToId ? currentChatMessages.find(m => m.id === msg.replyToId) : null

                return (
                  <div key={index} className={`message-wrapper ${isMe ? 'me' : 'them'}`}>
                    <div className="message-bubble" onDoubleClick={() => setActiveReactionMsg(msg.id)}>
                      {!isMe && (
                        <div className="sender-header">
                          <span className="sender-name" style={{ margin: 0, display: 'inline', fontWeight: 'bold', fontSize: '13px' }}>{msg.sender}</span>
                          {selectedChat === 'GLOBAL' && !friends.includes(msg.sender) && (
                            <button className="add-from-chat-btn" onClick={(e) => { e.stopPropagation(); sendFriendRequest(msg.sender); }}>➕</button>
                          )}
                          {!isMe && (
                            <button className="block-from-chat-btn" onClick={(e) => { e.stopPropagation(); blockUser(msg.sender); }}>🚫</button>
                          )}
                        </div>
                      )}

                      {repliedToMsg && (
                        <div className="replied-snippet">
                          <strong>{repliedToMsg.sender}</strong>
                          <p>{repliedToMsg.content.startsWith('[FILE]') ? '📎 Attachment' : repliedToMsg.content}</p>
                        </div>
                      )}

                      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
                        {renderMessageContent(msg.content)}
                        <div style={{ display: 'flex', alignItems: 'center', marginLeft: '10px' }}>
                          {isMe && selectedChat !== 'GLOBAL' && getStatusIcon(msg.messageStatus)}
                        </div>
                      </div>
                      
                      {reactionsObj && (
                        <div className="reactions-display">
                          {Object.entries(reactionsObj).map(([u, r]) => (
                            <span key={u} title={u}>{r}</span>
                          ))}
                        </div>
                      )}

                      {activeReactionMsg === msg.id && (
                        <div className="reaction-picker">
                          {['👍', '❤️', '😂', '🔥', '😢', '↩️'].map(emoji => (
                            <span key={emoji} onClick={() => {
                              if (emoji === '↩️') { setReplyingTo(msg); setActiveReactionMsg(null) }
                              else handleReaction(msg.id, emoji)
                            }}>{emoji}</span>
                          ))}
                        </div>
                      )}

                      {isMe && msg.id && (
                        <button className="delete-btn" onClick={() => {
                          if(ws.current) ws.current.send(JSON.stringify({ type: 'DELETE_MESSAGE', sender: username, receiver: selectedChat === 'GLOBAL' ? 'ALL' : selectedChat, content: msg.id.toString() }))
                        }} title="Delete Message">🗑️</button>
                      )}
                    </div>
                  </div>
                )
              })
            )}
            <div ref={messagesEndRef} />
          </div>
          
          {replyingTo && (
            <div className="replying-to-banner">
              <div>
                <strong>Replying to {replyingTo.sender}</strong>
                <p>{replyingTo.content.startsWith('[FILE]') ? '📎 Attachment' : replyingTo.content}</p>
              </div>
              <button onClick={() => setReplyingTo(null)}>❌</button>
            </div>
          )}

          {!isGameOpen && (
            <form className="input-area" onSubmit={sendMessage}>
              <input type="file" ref={fileInputRef} style={{ display: 'none' }} onChange={handleFileSelect} />
              <button type="button" className="attach-btn" onClick={() => fileInputRef.current.click()} title="Attach File">📎</button>
              <input type="text" placeholder="Type your message here..." value={currentMessage} onChange={handleTyping} />
              <button type="submit">Send</button>
            </form>
          )}
        </div>
      </div>
    </div>
  )
}

export default App
