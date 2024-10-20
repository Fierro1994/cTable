document.addEventListener('DOMContentLoaded', () => {
  const roomId = new URLSearchParams(window.location.search).get('roomId');
  const usernameElement = document.getElementById('username');
  const username = usernameElement ? usernameElement.value : 'unknown';

  let gameChatSocket;
  const generalChatBox = document.getElementById('generalChat');
  const teamChatBox = document.getElementById('teamChat');

  function connectChatWebSocket() {
    gameChatSocket = new WebSocket(`ws://${location.host}/gamechat?username=${username}&roomId=${roomId}`);

    gameChatSocket.onopen = () => {
      console.log('WebSocket подключен к /gamechat');
    };

    gameChatSocket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      console.log("Получено сообщение:", message);  // Лог для отладки
      displayChatMessage(message);
    };

    gameChatSocket.onclose = (event) => {
      console.log('WebSocket чата закрыт', event.code, event.reason);
    };

    gameChatSocket.onerror = (error) => {
      console.error('Ошибка WebSocket чата:', error);
    };
  }

  function displayChatMessage(message) {
    // Исправляем использование свойства на правильное
    const chatBox = message.teamMessage ? teamChatBox : generalChatBox;

    const messageElement = document.createElement('div');
    messageElement.textContent = `${message.sender}: ${message.content}`;
    chatBox.appendChild(messageElement);
    chatBox.scrollTop = chatBox.scrollHeight;  // Прокрутка вниз
  }

  // Делаем функции глобальными через объект window
  window.sendGeneralMessage = function() {
    const message = document.getElementById('generalChatMessage').value.trim();
    if (message && gameChatSocket.readyState === WebSocket.OPEN) {
      const chatMessage = {
        type: 'CHAT_MESSAGE',
        content: message,
        isTeamMessage: false,
        sender: username
      };
      gameChatSocket.send(JSON.stringify(chatMessage));
      document.getElementById('generalChatMessage').value = ''; // Очистка поля ввода
    }
  };

  window.sendTeamMessage = function() {
    const message = document.getElementById('teamChatMessage').value.trim();
    if (message && gameChatSocket.readyState === WebSocket.OPEN) {
      const chatMessage = {
        type: 'CHAT_MESSAGE',
        content: message,
        isTeamMessage: true,  // Убедитесь, что здесь true
        sender: username
      };
      gameChatSocket.send(JSON.stringify(chatMessage));
      document.getElementById('teamChatMessage').value = ''; // Очистка поля ввода
    }
  };

  // Функция для переключения вкладок
  window.switchChatTab = function (tab) {
    if (tab === 'general') {
      document.getElementById('generalChatTab').classList.add('active-tab');
      document.getElementById('teamChatTab').classList.remove('active-tab');
      document.getElementById('generalChatSection').classList.add('active-chat');
      document.getElementById('teamChatSection').classList.remove('active-chat');
    } else if (tab === 'team') {
      document.getElementById('teamChatTab').classList.add('active-tab');
      document.getElementById('generalChatTab').classList.remove('active-tab');
      document.getElementById('teamChatSection').classList.add('active-chat');
      document.getElementById('generalChatSection').classList.remove('active-chat');
    }
  };

  connectChatWebSocket();
});