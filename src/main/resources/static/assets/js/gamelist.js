document.addEventListener('DOMContentLoaded', function() {


  let rooms = [];
  let currentRoom = null;
  console.log("sadsf")
  function openCreateRoomModal() {
    console.log('Открываем модальное окно создания комнаты');
    document.getElementById('createRoomModal').style.display = 'block';
  }

  function closeCreateRoomModal() {
    console.log('Закрываем модальное окно создания комнаты');
    document.getElementById('createRoomModal').style.display = 'none';
  }

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

  async function refreshRooms() {
    console.log('Обновляем список комнат');
    try {
      const response = await fetch('/api/rooms');
      rooms = await response.json();
      displayRooms();
    } catch (error) {
      console.error('Ошибка при обновлении списка комнат:', error);
      alert('Ошибка при обновлении списка комнат');
    }
  }

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

  async function startGame(roomId) {
    try {
      const response = await fetch(`/api/rooms/${roomId}/start`, { method: 'POST' });
      if (response.ok) {
        alert('Игра начнется через 10 секунд!');
        // Здесь можно добавить логику для отсчета и начала игры
      } else {
        alert('Ошибка при начале игры');
      }
    } catch (error) {
      console.error('Ошибка при начале игры:', error);
      alert('Ошибка при начале игры');
    }
  }

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

  // Функция инициализации
  function initializeGameRooms() {
    console.log('Инициализация игровых комнат');
    const refreshBtn = document.getElementById('refreshRoomsBtn');
    const createRoomBtn = document.getElementById('createRoomBtn');
    const createRoomSubmitBtn = document.getElementById('createRoomSubmitBtn');
    const createRoomCancelBtn = document.getElementById('createRoomCancelBtn');

    if (refreshBtn) {
      console.log('Назначаем обработчик для кнопки обновления');
      refreshBtn.addEventListener('click', function() {
        console.log('Кнопка обновления нажата');
        refreshRooms();
      });
    } else {
      console.error('Кнопка обновления не найдена');
    }

    if (createRoomBtn) {
      console.log('Назначаем обработчик для кнопки создания комнаты');
      createRoomBtn.addEventListener('click', function() {
        console.log('Кнопка создания комнаты нажата');
        openCreateRoomModal();
      });
    } else {
      console.error('Кнопка создания комнаты не найдена');
    }

    if (createRoomSubmitBtn) {
      console.log('Назначаем обработчик для кнопки подтверждения создания комнаты');
      createRoomSubmitBtn.addEventListener('click', createRoom);
    } else {
      console.error('Кнопка подтверждения создания комнаты не найдена');
    }

    if (createRoomCancelBtn) {
      console.log('Назначаем обработчик для кнопки отмены создания комнаты');
      createRoomCancelBtn.addEventListener('click', closeCreateRoomModal);
    } else {
      console.error('Кнопка отмены создания комнаты не найдена');
    }

    // Инициализация при загрузке страницы
    refreshRooms();
  }

  // Вызываем функцию инициализации при загрузке DOM
  document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM полностью загружен');
    initializeGameRooms();
  });

  if (document.readyState === 'complete' || document.readyState === 'interactive') {
    console.log('DOM уже загружен, инициализируем немедленно');
    initializeGameRooms();
  }
  // Настройка WebSocket для обновлений в реальном времени
  socket.onmessage = function(event) {
    const data = JSON.parse(event.data);
    if (data.type === 'ROOM_UPDATE') {
      if (currentRoom && currentRoom.id === data.roomId) {
        displayRoomDetails(data.room);
      } else {
        refreshRooms();
      }
    } else if (data.type === 'GAME_STARTED' && currentRoom && currentRoom.id === data.roomId) {
      alert('Игра начинается!');
      // Здесь можно добавить логику для перехода к игровому интерфейсу
    }
  };
});