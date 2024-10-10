
let socket = null;
const messageInput = document.querySelector('#message-input');

console.log("Username from server:", username);
document.getElementById('username-display').textContent = username;

function connect() {
  socket = new WebSocket('ws://localhost:8080/chat');

  socket.onopen = () => {
    console.log("WebSocket connected");
    const joinMessage = {
      type: 'JOIN',
      sender: username,
      content: username + ' присоединился к чату'
    };
    socket.send(JSON.stringify(joinMessage));
  };

  socket.onmessage = (event) => {
    console.log("Received message:", event.data);
    try {
      const message = JSON.parse(event.data);
      renderMessage(message);
    } catch (error) {
      console.error("Error parsing message:", error);
    }
  };

  socket.onclose = () => {
    console.log("WebSocket disconnected");
    setTimeout(connect, 5000);
  };
}

connect();

function sendMessage() {
  const messageContent = messageInput.value.trim();

  if (messageContent && socket.readyState === WebSocket.OPEN) {
    const chatMessage = {
      type: 'CHAT',
      sender: username,
      content: messageContent
    };

    console.log("Sending message:", chatMessage);
    socket.send(JSON.stringify(chatMessage));
    messageInput.value = '';
  }
}

function renderMessage(message) {
  const chatMessages = document.getElementById('chat-messages');
  const messageElement = document.createElement('div');
  messageElement.classList.add('message');

  if (message.type === 'JOIN' || message.type === 'LEAVE') {
    messageElement.classList.add('system-message');
    messageElement.textContent = message.content;
  } else {
    if (message.sender === username) {
      messageElement.classList.add('own-message');
    }

    const usernameSpan = document.createElement('span');
    usernameSpan.classList.add('username');
    usernameSpan.textContent = message.sender + ': ';

    const contentSpan = document.createElement('span');
    contentSpan.classList.add('content');
    contentSpan.textContent = message.content;

    messageElement.appendChild(usernameSpan);
    messageElement.appendChild(contentSpan);
  }

  chatMessages.appendChild(messageElement);
  chatMessages.scrollTop = chatMessages.scrollHeight;
}

// Добавляем обработчик Enter для отправки сообщения
messageInput.addEventListener('keypress', function(event) {
  if (event.key === 'Enter') {
    sendMessage();
  }
});

window.addEventListener('beforeunload', function() {
  const chatMessages = document.getElementById('chat-messages');
  chatMessages.innerHTML = '';
});
