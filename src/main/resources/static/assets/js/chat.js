let chatSocket = null;
const messageInput = document.querySelector('#message-input');

function connectToChat() {
  chatSocket = new WebSocket('ws://localhost:8080/chat');

  chatSocket.onopen = () => {
    const joinMessage = {
      type: 'JOIN',
      sender: username,
      content: username + ' присоединился к чату'
    };
    chatSocket.send(JSON.stringify(joinMessage));
  };

  chatSocket.onmessage = (event) => {
    try {
      const message = JSON.parse(event.data);
      renderMessage(message);
    } catch (error) {
      console.error("Error parsing chat message:", error);
    }
  };

  chatSocket.onclose = () => {
    setTimeout(connectToChat, 5000);
  };
}

connectToChat();

function sendMessage() {
  const messageContent = messageInput.value.trim();

  if (messageContent && chatSocket.readyState === WebSocket.OPEN) {
    const chatMessage = {
      type: 'CHAT',
      sender: username,
      content: messageContent
    };

    chatSocket.send(JSON.stringify(chatMessage));
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

messageInput.addEventListener('keypress', function(event) {
  if (event.key === 'Enter') {
    sendMessage();
  }
});

window.addEventListener('beforeunload', function() {
  const chatMessages = document.getElementById('chat-messages');
  chatMessages.innerHTML = '';
});