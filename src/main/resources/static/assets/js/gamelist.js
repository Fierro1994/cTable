document.addEventListener('DOMContentLoaded', function() {
  let rooms = [];
  let currentRoom = null;
  let roomsSocket = null;
  let username = document.getElementById('username').value;

  initializeGameRooms();

  const savedRoomId = localStorage.getItem('currentRoomId');
  if (savedRoomId) {
    // Если в localStorage есть сохраненный roomId, пытаемся присоединиться к комнате
    joinRoom(savedRoomId)
      .catch(() => {
      console.log('Не удалось восстановить комнату, обновляем список комнат.');
      refreshRooms();
    });
  } else {
    // Если нет сохраненной комнаты, просто обновляем список комнат
    refreshRooms();
  }

  // Функция для открытия WebSocket подключения
  function connectToRoomsSocket() {
    if (roomsSocket && (roomsSocket.readyState === WebSocket.OPEN || roomsSocket.readyState === WebSocket.CONNECTING)) {
      console.log("WebSocket уже подключен или в процессе подключения, readyState:", roomsSocket.readyState);
      return;  // Прекращаем попытку подключения, если сокет уже открыт или подключается
    }

    console.log("Создание нового WebSocket соединения");
    roomsSocket = new WebSocket('ws://localhost:8080/rooms');

    roomsSocket.onopen = () => {
      console.log("WebSocket открыт, readyState:", roomsSocket.readyState);
      refreshRooms();  // Обновляем список комнат при успешном подключении
    };

    roomsSocket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      handleRoomMessage(message);
    };

    roomsSocket.onclose = (event) => {
      console.log("WebSocket закрыт, код:", event.code, "причина:", event.reason);
      // Можно добавить логику автоматического переподключения через какое-то время
    };

    roomsSocket.onerror = (error) => {
      console.error("Ошибка WebSocket:", error);
    };
  }

  function handleRoomMessage(message) {
    console.log("Обрабатываем сообщение:", message);
    switch (message.type) {
      case 'ROOM_UPDATE':
        if (currentRoom && currentRoom.id === message.roomId) {
          currentRoom = JSON.parse(message.content);
          displayRoomDetails(currentRoom);
        }
        break;
      case 'GAME_STARTED':
        if (currentRoom && currentRoom.id === message.roomId) {
          alert('Игра начинается!');
        }
        break;
      case 'ROOM_DISBANDED':
        console.log("Получено сообщение о роспуске комнаты:", message);

        // Проверяем, что текущая комната — это та, которая была распущена
        if (currentRoom && currentRoom.id.toString() === message.content) {
          alert('Комната была распущена');
          currentRoom = null;  // Очищаем информацию о текущей комнате
        }

        // Обновляем список доступных комнат
        refreshRooms();
        break;
      case 'ROOM_LIST_UPDATE':  // Обработка обновления списка комнат
        console.log("Получено обновление списка комнат:", message);
        rooms = JSON.parse(message.content);  // Обновляем список комнат
        displayRooms();  // Отображаем комнаты
        break;
      default:
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
    try {
      const name = document.getElementById('roomName').value;
      const maxPlayers = parseInt(document.getElementById('maxPlayers').value, 10);
      const category = document.getElementById('roomCategory').value;

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
        currentRoom.playerIds = [username]; // Добавляем создателя в список игроков

        // Сохраняем ID созданной комнаты в localStorage
        localStorage.setItem('currentRoomId', createdRoom.id);

        // Закрываем модальное окно создания комнаты
        closeCreateRoomModal();

        // Отправляем событие через WebSocket
        const createdEvent = {
          type: 'ROOM_CREATED',
          sender: username,
          content: JSON.stringify(currentRoom)
        };
        roomsSocket.send(JSON.stringify(createdEvent));

        // Отображаем информацию о новой комнате сразу после её создания
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

    // Если текущая комната выбрана, показываем только её детали
    if (currentRoom) {
      displayRoomDetails(currentRoom);
      return;
    }

    // Если текущая комната не выбрана, отображаем список доступных комнат
    roomsContainer.innerHTML = `
    <div class="game-rooms-header">
      <h2 class="game-rooms-title">Игровые комнаты</h2>
      <button class="refresh-button" id="refreshRoomsBtn">Обновить</button>
      <button class="create-room-button" id="createRoomBtn">Создать комнату</button>
    </div>
    <div id="gameRoomsList" class="game-rooms-list">
      <h3>Доступные комнаты</h3>
      ${rooms.length === 0 ? '<p>Нет доступных комнат</p>' : ''}
    </div>
  `;

    const gameRoomsList = document.getElementById('gameRoomsList');

    if (rooms.length > 0) {
      rooms.forEach(room => {
        const roomElement = document.createElement('div');
        roomElement.className = 'game-room';
        roomElement.innerHTML = `
        <div class="game-room-name">${room.name}</div>
        <div class="game-room-info">Игроки: ${room.playerIds.length}/${room.maxPlayers}</div>
        <div class="game-room-info">Категория: ${room.category}</div>
        <button class="join-button" data-room-id="${room.id}">Присоединиться</button>
      `;
        gameRoomsList.appendChild(roomElement);
      });
    }

    // Добавляем обработчики событий для кнопок
    document.getElementById('refreshRoomsBtn').addEventListener('click', refreshRooms);
    document.getElementById('createRoomBtn').addEventListener('click', openCreateRoomModal);
    document.querySelectorAll('.join-button').forEach(button => {
      button.addEventListener('click', () => joinRoom(button.dataset.roomId));
    });
  }


  // Функция для присоединения к комнате
  async function joinRoom(roomId) {
    try {
      const response = await fetch(`/api/rooms/${roomId}`);
      if (!response.ok) {
        if (response.status === 404) {
          console.error('Комната не найдена. Возможно, она была удалена.');
          localStorage.removeItem('currentRoomId');  // Удаляем из localStorage
          refreshRooms();  // Обновляем список комнат
          return;
        } else {
          alert('Ошибка при получении данных комнаты');
          return;
        }
      }

      const room = await response.json();

      // Проверяем, есть ли уже текущий пользователь в списке игроков
      if (room.playerIds && room.playerIds.includes(username)) {
        console.log('Вы уже находитесь в этой комнате.');
        currentRoom = room;  // Обновляем состояние текущей комнаты
        displayRoomDetails(room);  // Отображаем информацию о комнате
        return;  // Выход, если пользователь уже в комнате
      }

      // Если игрока нет в комнате, присоединяемся к ней
      const joinResponse = await fetch(`/api/rooms/${roomId}/join`, { method: 'POST' });
      if (joinResponse.ok) {
        currentRoom = await joinResponse.json();
        displayRoomDetails(currentRoom);

        // Сохраняем текущую комнату в localStorage
        localStorage.setItem('currentRoomId', roomId);

        const joinEvent = {
          type: 'ROOM_JOINED',
          sender: username,
          content: roomId.toString()
        };
        roomsSocket.send(JSON.stringify(joinEvent));
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
      const response = await fetch(`/api/rooms/${roomId}/disband`, { method: 'POST' });
      if (response.ok) {
        console.log('Комната успешно распущена');
        currentRoom = null;

        // Удаляем текущую комнату из localStorage
        localStorage.removeItem('currentRoomId');

        const disbandEvent = {
          type: 'ROOM_DISBANDED',
          sender: username,
          content: roomId.toString()
        };
        roomsSocket.send(JSON.stringify(disbandEvent));

        refreshRooms();
        displayRooms();
      } else {
        console.error('Ошибка при роспуске комнаты:', response.status, response.statusText);
        alert('Ошибка при роспуске комнаты: ' + response.statusText);
      }
    } catch (error) {
      console.error('Ошибка при роспуске комнаты:', error);
      alert('Ошибка при роспуске комнаты: ' + error.message);
    }
  }

  // Функция для выхода из комнаты
  async function leaveRoom(roomId) {
    try {
      const response = await fetch(`/api/rooms/${roomId}/leave`, { method: 'POST' });
      if (response.ok) {
        currentRoom = null;
        refreshRooms();

        // Удаляем текущую комнату из localStorage
        localStorage.removeItem('currentRoomId');

        const leaveEvent = {
          type: 'ROOM_LEFT',
          sender: username,
          content: roomId.toString()
        };
        roomsSocket.send(JSON.stringify(leaveEvent));
      } else {
        alert('Ошибка при выходе из комнаты');
      }
    } catch (error) {
      console.error('Ошибка при выходе из комнаты:', error);
      alert('Ошибка при выходе из комнаты');
    }
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

    // Обязательно вызываем refreshRooms при инициализации
    connectToRoomsSocket();
    refreshRooms();  // Обновляем список комнат после подключения
  }

  document.addEventListener('DOMContentLoaded', initializeGameRooms);
});