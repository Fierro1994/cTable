document.addEventListener('DOMContentLoaded', function() {
  let rooms = [];
  let currentRoom = null;
  let roomsSocket = null;
  let username = document.getElementById('username').value;


  initializeGameRooms();

  const savedRoomId = localStorage.getItem('currentRoomId');
  if (savedRoomId) {
    joinRoom(savedRoomId).catch(() => {
      console.log('Не удалось восстановить комнату, обновляем список комнат.');
      refreshRooms();
    });
  } else {
    refreshRooms();
  }

  function connectToRoomsSocket() {
    if (roomsSocket && (roomsSocket.readyState === WebSocket.OPEN || roomsSocket.readyState === WebSocket.CONNECTING)) {
      console.log("WebSocket уже подключен или в процессе подключения, readyState:", roomsSocket.readyState);
      return;
    }

    console.log("Создание нового WebSocket соединения");
    roomsSocket = new WebSocket(`ws://localhost:8080/rooms?username=${username}`);

    roomsSocket.onopen = () => {
      console.log("WebSocket открыт, readyState:", roomsSocket.readyState);
      refreshRooms();
    };

    roomsSocket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      handleRoomMessage(message);
    };

    roomsSocket.onclose = (event) => {
      console.log("WebSocket закрыт, код:", event.code, "причина:", event.reason);
    };

    roomsSocket.onerror = (error) => {
      console.error("Ошибка WebSocket:", error);
    };
  }



  function handleRoomMessage(message) {
    console.log("Обрабатываем сообщение:", message);
    switch (message.type) {
      case 'ROOM_LEFT_CONFIRMATION':
        currentRoom = null;
        localStorage.removeItem('currentRoomId');
        displayRooms();
        break;
      case 'ROOM_UPDATE':
        const updatedRoom = JSON.parse(message.content);
        if (currentRoom && currentRoom.id === updatedRoom.id) {
          currentRoom = updatedRoom;
          displayRoomDetails(currentRoom);
        }
        break;
      case 'GAME_STARTED':
        if (currentRoom && currentRoom.id === message.roomId) {
          alert('Игра начинается!');
        }
        break;
      case 'ROOM_DISBANDED':
        handleRoomDisbanded();
        break;
      case 'ROOM_LIST_UPDATE':
        console.log("Получено обновление списка комнат:", message);
        rooms = JSON.parse(message.content);
        if (rooms.length === 0) {
          console.log("Список комнат пуст");
        }
        if (!currentRoom) {
          displayRooms();
        }
        break;
      case 'ROOM_JOIN_CONFIRMATION':
        const joinedRoom = JSON.parse(message.content);
        currentRoom = joinedRoom;
        localStorage.setItem('currentRoomId', joinedRoom.id);
        displayRoomDetails(currentRoom);
        break;
      default:
        console.log("Неизвестный тип сообщения:", message.type);
    }
  }


  function handleRoomDisbanded() {
    currentRoom = null;
    localStorage.removeItem('currentRoomId');
    displayRooms();
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
    try {
      const maxPlayers = parseInt(document.getElementById('maxPlayers').value, 10);
      const category = document.getElementById('roomCategory').value;
      const name = username + "'s Room";

      const response = await fetch('/api/rooms', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ name, maxPlayers, category })
      });

      if (response.ok) {
        const createdRoom = await response.json();
        currentRoom = createdRoom;
        localStorage.setItem('currentRoomId', createdRoom.id);
        closeCreateRoomModal();

        const createdEvent = {
          type: 'ROOM_CREATED',
          sender: username,
          content: createdRoom.id.toString()
        };
        roomsSocket.send(JSON.stringify(createdEvent));

        displayRoomDetails(currentRoom);
      } else {
        const errorData = await response.json();
        alert(`Ошибка при создании комнаты: ${errorData.message || response.statusText}`);
      }
    } catch (error) {
      console.error('Ошибка при отправке запроса:', error);
      alert('Произошла ошибка при создании комнаты');
    }
  }
  // Функция для получения значения куки по имени
  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
  }


  // Функция для отображения списка комнат

  function displayRooms() {
    const roomsContainer = document.querySelector('.game-rooms-display');

    if (currentRoom) {
      displayRoomDetails(currentRoom);
      return;
    }

    roomsContainer.innerHTML = `
            <div class="game-rooms-header">
                <h2 class="game-rooms-title">Игровые комнаты</h2>
                <button class="refresh-button" id="refreshRoomsBtn">Обновить</button>
                <button class="create-room-button" id="createRoomBtn">Создать комнату</button>
            </div>
            <table id="gameRoomsList" class="game-rooms-table">
                <thead>
                    <tr>
                        <th>Название комнаты</th>
                        <th>Категория</th>
                        <th>Игроки</th>
                        <th>Время</th>
                        <th>Действие</th>
                    </tr>
                </thead>
                <tbody>
                    ${rooms.length === 0 ? '<tr><td colspan="5">Нет доступных комнат. Вы можете создать новую!</td></tr>' : ''}
                </tbody>
            </table>
        `;

    const gameRoomsList = document.querySelector('#gameRoomsList tbody');

    if (rooms.length > 0) {
      rooms.forEach(room => {
        const roomElement = document.createElement('tr');
        const timeLeft = getTimeLeft(room.createdAt);
        roomElement.innerHTML = `
        <td>${room.name}</td>
        <td>${room.category}</td>
        <td>${room.playerIds.length}/${room.maxPlayers}</td>
        <td class="room-timer" data-created="${room.createdAt}">${timeLeft}</td>
        <td><button class="join-button" data-room-id="${room.id}">Присоединиться</button></td>
      `;
        gameRoomsList.appendChild(roomElement);
      });
    }

    document.getElementById('refreshRoomsBtn').addEventListener('click', refreshRooms);
    document.getElementById('createRoomBtn').addEventListener('click', openCreateRoomModal);
    document.querySelectorAll('.join-button').forEach(button => {
      button.addEventListener('click', () => joinRoom(button.dataset.roomId));
    });

    startRoomTimers();
  }




  function getTimeLeft(createdAt) {
    const now = new Date();
    const created = new Date(createdAt);
    const timePassed = now - created;
    const timeLeft = Math.max(0, 20 * 60 * 1000 - timePassed);
    const minutes = Math.floor(timeLeft / (60 * 1000));
    const seconds = Math.floor((timeLeft % (60 * 1000)) / 1000);
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }

  function startRoomTimers() {
    setInterval(() => {
      document.querySelectorAll('.room-timer').forEach(timer => {
        const createdAt = timer.dataset.created;
        timer.textContent = getTimeLeft(createdAt);
        if (timer.textContent === '00:00') {
          const roomId = timer.closest('tr').querySelector('.join-button').dataset.roomId;
          disbandRoom(roomId);
        }
      });
    }, 1000);
  }

  // Функция для присоединения к комнате


  async function joinRoom(roomId) {
    if (roomsSocket && roomsSocket.readyState === WebSocket.OPEN) {
      const joinEvent = {
        type: 'ROOM_JOINED',
        sender: username,
        content: roomId.toString()
      };
      roomsSocket.send(JSON.stringify(joinEvent));
    } else {
      console.error("WebSocket не подключен. Попытка повторного подключения...");
      connectToRoomsSocket();
    }
  }

  // Функция для отображения деталей комнаты
  function displayRoomDetails(room) {
    const roomsList = document.getElementById('gameRoomsList');
    if (!room) {
      roomsList.innerHTML = '<p>Вы не находитесь в комнате</p>';
      return;
    }

    roomsList.innerHTML = `
    <h3>Комната: ${room.name}</h3>
    <div>Категория: ${room.category}</div>
    <div>Игроки: ${room.playerIds.length}/${room.maxPlayers}</div>
    <ul>
      ${room.playerIds.map(playerId => `<li>${playerId}</li>`).join('')}
    </ul>
    ${room.creatorId === username ?
        `<button id="startGameBtn">Начать игру</button>
         <button id="disbandRoomBtn">Распустить комнату</button>` :
        `<button id="leaveRoomBtn">Покинуть комнату</button>`
    }
    `;

    if (room.creatorId === username) {
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
        const disbandEvent = {
          type: 'ROOM_DISBANDED',
          sender: username,
          content: roomId.toString()
        };
        roomsSocket.send(JSON.stringify(disbandEvent));
        handleRoomDisbanded();

    } catch (error) {
      console.error('Ошибка при роспуске комнаты:', error);
      alert('Ошибка при роспуске комнаты: ' + error.message);
    }
  }

  // Функция для выхода из комнаты
  function leaveRoom(roomId) {
    const event = {
      type: 'ROOM_LEFT',
      content: roomId.toString()
    };
    roomsSocket.send(JSON.stringify(event));

    // Добавляем обновление списка комнат после выхода
    refreshRooms();
  }

  function refreshRooms() {
    if (roomsSocket && roomsSocket.readyState === WebSocket.OPEN) {
      const refreshEvent = {
        type: 'ROOM_LIST_REQUEST',
        sender: username,
        content: 'refresh'
      };
      roomsSocket.send(JSON.stringify(refreshEvent));  // Запрос списка комнат
    } else if (!roomsSocket || roomsSocket.readyState === WebSocket.CLOSED) {
      console.error("WebSocket не подключен. Попытка повторного подключения...");
      connectToRoomsSocket();  // Попробуй переподключиться
    }
  }
  // Инициализация кнопок и обновление списка комнат
  function initializeGameRooms() {
    console.log('Инициализация игровых комнат');

    const refreshBtn = document.getElementById('refreshRoomsBtn');
    const createRoomBtn = document.getElementById('createRoomBtn');
    const createRoomSubmitBtn = document.getElementById('createRoomSubmitBtn');
    const createRoomCancelBtn = document.getElementById('createRoomCancelBtn');

    if (refreshBtn) refreshBtn.addEventListener('click', refreshRooms);
    if (createRoomBtn) createRoomBtn.addEventListener('click', openCreateRoomModal);
    if (createRoomSubmitBtn) createRoomSubmitBtn.addEventListener('click', createRoom);
    if (createRoomCancelBtn) createRoomCancelBtn.addEventListener('click', closeCreateRoomModal);

    connectToRoomsSocket();
    refreshRooms();
  }

  document.addEventListener('DOMContentLoaded', initializeGameRooms);
});