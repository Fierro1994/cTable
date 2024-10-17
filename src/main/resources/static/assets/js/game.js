document.addEventListener('DOMContentLoaded', () => {
  const game_socket = new WebSocket('ws://localhost:8080/games?username=${username}'); // Подключение к новому сокету

  game_socket.onopen = () => {
    console.log('WebSocket подключен к /games');
    // Можно отправить запрос на получение состояния игры
    requestGameState();
  };

  game_socket.onmessage = (event) => {
    const message = JSON.parse(event.data);
    handleWebSocketMessage(message);
  };

  game_socket.onclose = () => {
    console.log('WebSocket закрыт');
  };

  game_socket.onerror = (error) => {
    console.error('Ошибка WebSocket:', error);
  };

  // Отправка запроса на получение состояния игры
  function requestGameState() {
    const event = {
      type: 'GET_GAME_STATE',
      content: '' // Укажите ID комнаты, если нужно
    };
    game_socket.send(JSON.stringify(event));
  }

  // Обработка сообщений WebSocket
  function handleWebSocketMessage(message) {
    switch (message.type) {
      case 'GAME_STATE':
        updateGameState(JSON.parse(message.content));
        break;
      case 'GAME_STARTED':
        alert('Игра началась!');
        break;
      case 'ERROR':
        console.error('Ошибка от сервера:', message.content);
        break;
      default:
        console.warn('Неизвестное сообщение:', message);
    }
  }

  // Обновление состояния игры (категория и команды)
  function updateGameState(gameState) {
    document.getElementById('gameCategory').textContent = gameState.category || 'Неизвестно';

    updateTeamList(document.getElementById('teamA'), gameState.teamA);
    updateTeamList(document.getElementById('teamB'), gameState.teamB);
    updateScores(gameState.teamAScore, gameState.teamBScore);
  }

  // Обновление списка игроков команды
  function updateTeamList(listElement, players) {
    listElement.innerHTML = '';
    players.forEach(player => {
      const li = document.createElement('li');
      li.textContent = player;
      listElement.appendChild(li);
    });
  }

  // Обновление счёта команд
  function updateScores(teamAScore, teamBScore) {
    document.getElementById('teamAScore').textContent = teamAScore;
    document.getElementById('teamBScore').textContent = teamBScore;
  }

  // Пример: Отправка сообщения о старте игры
  function startGame() {
    const event = {
      type: 'START_GAME',
      content: '' // Укажите ID комнаты, если нужно
    };
    game_socket.send(JSON.stringify(event));
  }

  // При необходимости привязываем события к кнопкам или другим элементам
  // Например: document.getElementById('startGameButton').addEventListener('click', startGame);
});