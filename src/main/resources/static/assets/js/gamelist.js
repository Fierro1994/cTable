document.addEventListener('DOMContentLoaded', function() {
  let rooms = [];
  let currentRoom = null;
  let roomsSocket = null; // WebSocket для игровых комнат
  initializeGameRooms();
  // Функция для открытия WebSocket подключения
  function connectToRoomsSocket() {
    console.log("Попытка подключения к WebSocket для игровых комнат...");

    roomsSocket = new WebSocket('ws://localhost:8080/rooms');

    roomsSocket.onopen = () => {
      console.log("WebSocket для комнат успешно подключен");
      // Можно отправить запрос на получение списка комнат, если это требуется
      const roomRequest = {
        type: 'ROOM_LIST_REQUEST',
        sender: username
      };
      roomsSocket.send(JSON.stringify(roomRequest));
      console.log("Запрос на список комнат отправлен:", roomRequest);
    };

    roomsSocket.onmessage = (event) => {
      console.log("Получено сообщение по WebSocket комнат:", event.data);
      try {
        const message = JSON.parse(event.data);
        handleRoomMessage(message); // Обработка сообщения от WebSocket
      } catch (error) {
        console.error("Ошибка при разборе сообщения от WebSocket комнат:", error);
      }
    };

    roomsSocket.onerror = (error) => {
      console.error("Ошибка WebSocket для комнат:", error);
    };

    roomsSocket.onclose = () => {
      console.log("WebSocket для комнат отключен");
      // Переподключение через 5 секунд при отключении
      setTimeout(connectToRoomsSocket, 5000);
    };
  }

  // Функция для обработки сообщений от WebSocket
  function handleRoomMessage(message) {
    console.log("Обрабатываем сообщение:", message);
    if (message.type === 'ROOM_LIST_UPDATE') {
      refreshRooms(); // Обновляем список комнат при обновлении
    } else if (message.type === 'ROOM_UPDATE' && currentRoom && currentRoom.id === message.roomId) {
      displayRoomDetails(message.room); // Обновляем детали комнаты
    } else if (message.type === 'GAME_STARTED' && currentRoom && currentRoom.id === message.roomId) {
      alert('Игра начинается!');
      // Здесь можно добавить логику для начала игры
    } else {
      console.log("Неизвестный тип сообщения:", message.type);
    }
  }

  // Функция для открытия и закрытия модального окна создания комнаты
  function openCreateRoomModal() {
    console.log('Открываем модальное окно создания комнаты');
    document.getElementById('createRoomModal').style.display = 'block';
  }

  function closeCreateRoomModal() {
    console.log('Закрываем модальное окно создания комнаты');
    document.getElementById('createRoomModal').style.display = 'none';
  }

  // Функция для создания новой комнаты
  async function createRoom() {
    const name = document.getElementById('roomName').value;
    const maxPlayers = document.getElementById('maxPlayers').value;
    const category = document.getElementById('roomCategory').value;

    const response = await fetch('/api/rooms', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name, maxPlayers, category })
    });

    if (response.ok) {
      closeCreateRoomModal();
      refreshRooms();
    } else {
      alert('Ошибка при создании комнаты');
    }
  }

  // Функция для обновления списка комнат
  async function refreshRooms() {
    console.log('Обновляем список комнат');
    try {
      const response = await fetch('/api/rooms');
      if (response.ok) {
        rooms = await response.json();
        if (Array.isArray(rooms)) {
          displayRooms();
        } else {
          console.error('Получен некорректный ответ, ожидается массив:', rooms);
        }
      } else {
        console.error('Ошибка при запросе списка комнат:', response.status);
        alert('Ошибка при обновлении списка комнат');
      }
    } catch (error) {
      console.error('Ошибка при обновлении списка комнат:', error);
      alert('Ошибка при обновлении списка комнат');
    }
  }

  // Функция для отображения списка комнат
  function displayRooms() {
    const roomsList = document.getElementById('gameRoomsList');
    roomsList.innerHTML = '';
    rooms.forEach(room => {
      const roomElement = document.createElement('div');
      roomElement.className = 'game-room';
      roomElement.innerHTML = `
      <div class="game-room-name">${room.name}</div>
      <div class="game-room-info">Игроки: ${room.playerIds.length}/${room.maxPlayers}</div>
      <div class="game-room-info">Категория: ${room.category}</div>
      <button class="join-button" data-room-id="${room.id}">Присоединиться</button>
    `;
      roomsList.appendChild(roomElement);
    });

    // Добавляем обработчики для кнопок "Присоединиться"
    document.querySelectorAll('.join-button').forEach(button => {
      button.addEventListener('click', () => joinRoom(button.dataset.roomId));
    });
  }

  // Функция для присоединения к комнате
  async function joinRoom(roomId) {
    try {
      const response = await fetch(`/api/rooms/${roomId}/join`, { method: 'POST' });
      if (response.ok) {
        currentRoom = await response.json();
        displayRoomDetails(currentRoom);
      } else {
        alert('Ошибка при присоединении к комнате');
      }
    } catch (error) {
      console.error('Ошибка при присоединении к комнате:', error);
      alert('Ошибка при присоединении к комнате');
    }
  }

  // Функция для отображения деталей комнаты
  function displayRoomDetails(room) {
    const roomsList = document.getElementById('gameRoomsList');
    roomsList.innerHTML = `
    <h3>Комната: ${room.name}</h3>
    <div>Категория: ${room.category}</div>
    <div>Игроки: ${room.playerIds.length}/${room.maxPlayers}</div>
    <ul>
      ${room.playerIds.map(playerId => `<li>${playerId}</li>`).join('')}
    </ul>
    ${room.creatorId === /*[[${#authentication.name}]]*/ '' ?
      `<button id="startGameBtn">Начать игру</button>
       <button id="disbandRoomBtn">Распустить комнату</button>` :
      `<button id="leaveRoomBtn">Покинуть комнату</button>`
    }
  `;

    // Добавляем обработчики для новых кнопок
    if (room.creatorId === /*[[${#authentication.name}]]*/ '') {
      document.getElementById('startGameBtn').addEventListener('click', () => startGame(room.id));
      document.getElementById('disbandRoomBtn').addEventListener('click', () => disbandRoom(room.id));
    } else {
      document.getElementById('leaveRoomBtn').addEventListener('click', () => leaveRoom(room.id));
    }
  }

  // Функция для начала игры
  async function startGame(roomId) {
    try {
      const response = await fetch(`/api/rooms/${roomId}/start`, { method: 'POST' });
      if (response.ok) {
        alert('Игра начнется через 10 секунд!');
      } else {
        alert('Ошибка при начале игры');
      }
    } catch (error) {
      console.error('Ошибка при начале игры:', error);
      alert('Ошибка при начале игры');
    }
  }

  // Функция для роспуска комнаты
  async function disbandRoom(roomId) {
    try {
      const response = await fetch(`/api/rooms/${roomId}/disband`, { method: 'POST' });
      if (response.ok) {
        currentRoom = null;
        refreshRooms();
      } else {
        alert('Ошибка при роспуске комнаты');
      }
    } catch (error) {
      console.error('Ошибка при роспуске комнаты:', error);
      alert('Ошибка при роспуске комнаты');
    }
  }

  // Функция для выхода из комнаты
  async function leaveRoom(roomId) {
    try {
      const response = await fetch(`/api/rooms/${roomId}/leave`, { method: 'POST' });
      if (response.ok) {
        currentRoom = null;
        refreshRooms();
      } else {
        alert('Ошибка при выходе из комнаты');
      }
    } catch (error) {
      console.error('Ошибка при выходе из комнаты:', error);
      alert('Ошибка при выходе из комнаты');
    }
  }

  // Инициализация кнопок и обновление списка комнат
  function initializeGameRooms() {
    console.log('Инициализация игровых комнат');
    const refreshBtn = document.getElementById('refreshRoomsBtn');
    const createRoomBtn = document.getElementById('createRoomBtn');
    const createRoomSubmitBtn = document.getElementById('createRoomSubmitBtn');
    const createRoomCancelBtn = document.getElementById('createRoomCancelBtn');

    if (refreshBtn) {
      refreshBtn.addEventListener('click', refreshRooms);
    }

    if (createRoomBtn) {
      createRoomBtn.addEventListener('click', openCreateRoomModal);
    }

    if (createRoomSubmitBtn) {
      createRoomSubmitBtn.addEventListener('click', createRoom);
    }

    if (createRoomCancelBtn) {
      createRoomCancelBtn.addEventListener('click', closeCreateRoomModal);
    }

    refreshRooms(); // Первоначальное обновление списка комнат

    // Подключаемся к WebSocket для комнат
    connectToRoomsSocket();
  }

  document.addEventListener('DOMContentLoaded', initializeGameRooms);
});