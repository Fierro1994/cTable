
document.addEventListener('DOMContentLoaded', () => {
  const usernameElement = document.getElementById('username');
  const username = usernameElement ? usernameElement.value : 'unknown';
  const roomId = 2

  let game_socket;
  let reconnectAttempts = 0;
  const maxReconnectAttempts = 5;

  function connectWebSocket() {
    game_socket = new WebSocket(`ws://${location.host}/games?username=${encodeURIComponent(username)}`);

    game_socket.onopen = () => {
      console.log('WebSocket подключен к /games');
      reconnectAttempts = 0;
      requestGameState();
    };

    game_socket.onmessage = (event) => {
      console.log('Received message:', event.data);
      try {
        const message = JSON.parse(event.data);
        handleWebSocketMessage(message);
      } catch (error) {
        console.error('Ошибка при разборе сообщения:', error);
      }
    };

    game_socket.onclose = (event) => {
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

    game_socket.onerror = (error) => {
      console.error('Ошибка WebSocket:', error);
    };
  }

  connectWebSocket();

  function requestGameState() {
    if (game_socket.readyState === WebSocket.OPEN) {
      const event = {
        type: 'GET_GAME_STATE',
        content: roomId // Теперь отправляем roomId
      };
      game_socket.send(JSON.stringify(event));
    } else {
      console.warn('WebSocket не готов. Состояние:', game_socket.readyState);
    }
  }

  function handleWebSocketMessage(message) {
    switch (message.type) {
      case 'GAME_STATE':
      case 'GAME_STARTED':
      case 'SCORE_UPDATED':
        try {
          const gameState = JSON.parse(message.content);
          updateGameState(gameState);
          if (message.type === 'GAME_STARTED') {
            alert('Игра началась!');
          }
        } catch (error) {
          console.error('Ошибка при разборе состояния игры:', error);
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
    document.getElementById('gameCategory').textContent = gameState.category || 'Неизвестно';
    updateTeamList('teamA', gameState.teamA);
    updateTeamList('teamB', gameState.teamB);
    updateScores(gameState.teamAScore, gameState.teamBScore);
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

  function updateScores(teamAScore, teamBScore) {
    document.getElementById('teamAScore').textContent = teamAScore;
    document.getElementById('teamBScore').textContent = teamBScore;
  }
});
