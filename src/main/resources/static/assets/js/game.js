document.addEventListener('DOMContentLoaded', () => {
  const usernameElement = document.getElementById('username');
  const username = usernameElement ? usernameElement.value : 'unknown';
  const roomId = new URLSearchParams(window.location.search).get('roomId');

  let gameSocket;
  let reconnectAttempts = 0;
  const maxReconnectAttempts = 5;

  function connectWebSocket() {
    gameSocket = new WebSocket(`ws://${location.host}/games?username=${encodeURIComponent(username)}`);

    gameSocket.onopen = () => {
      console.log('WebSocket подключен к /games');
      reconnectAttempts = 0;
      requestGameState(); // Запрашиваем состояние игры сразу после подключения
    };

    gameSocket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data);
        handleWebSocketMessage(message);
      } catch (error) {
        console.error('Ошибка при разборе сообщения:', error);
      }
    };

    gameSocket.onclose = (event) => {
      console.log('WebSocket закрыт', event.code, event.reason);
      if (reconnectAttempts < maxReconnectAttempts) {
        setTimeout(() => {
          reconnectAttempts++;
          console.log(`Попытка переподключения ${reconnectAttempts}...`);
          connectWebSocket();
        }, 3000); // Пауза 3 секунды перед повторным подключением
      } else {
        console.error('Достигнуто максимальное количество попыток переподключения');
      }
    };

    gameSocket.onerror = (error) => {
      console.error('Ошибка WebSocket:', error);
    };
  }

  connectWebSocket();

  function requestGameState() {
    if (gameSocket.readyState === WebSocket.OPEN) {
      const event = {
        type: 'GET_GAME_STATE',
        content: roomId
      };
      gameSocket.send(JSON.stringify(event));
    } else {
      console.warn('WebSocket не готов. Состояние:', gameSocket.readyState);
    }
  }

  function handleWebSocketMessage(message) {
    switch (message.type) {
      case 'GAME_STATE':
      case 'GAME_STARTED':
      case 'SCORE_UPDATED':
        const gameState = JSON.parse(message.content);
        updateGameState(gameState);
        if (message.type === 'GAME_STARTED') {
          alert('Игра началась!');
        }
        break;
      case 'ERROR':
        console.error('Ошибка от сервера:', message.content);
        break;
      default:
        console.warn('Неизвестное сообщение:', message);
    }
  }

  function updateGameState(gameState) {
    document.querySelector('.game-category span').textContent = gameState.category || '-';
    document.querySelector('#teamAScore').textContent = gameState.teamAScore;
    document.querySelector('#teamBScore').textContent = gameState.teamBScore;

    updateTeamList('teamA', gameState.teamA);
    updateTeamList('teamB', gameState.teamB);
  }

  function updateTeamList(teamId, players) {
    const teamList = document.getElementById(teamId);
    teamList.innerHTML = '';
    players.forEach(player => {
      const li = document.createElement('li');
      li.textContent = player;
      teamList.appendChild(li);
    });
  }
});