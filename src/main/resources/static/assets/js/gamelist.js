document.addEventListener('DOMContentLoaded', function() {
  let rooms = [];
  let currentRoom = null;
  let roomsSocket = null;
  let username = document.getElementById('username').value;

  initializeGameRooms();

  function connectToRoomsSocket() {
    if (roomsSocket && (roomsSocket.readyState === WebSocket.OPEN || roomsSocket.readyState === WebSocket.CONNECTING)) {
      return;
    }

    roomsSocket = new WebSocket(`ws://localhost:8080/rooms?username=${username}`);
    roomsSocket.onopen = () => {
      refreshRooms();
    };

    roomsSocket.onmessage = (event) => {
      const message = JSON.parse(event.data);
      handleRoomMessage(message);
    };

    roomsSocket.onclose = (event) => {
    };

    roomsSocket.onerror = (error) => {
      console.error("Ошибка WebSocket:", error);
    };
  }



  function handleRoomMessage(message) {
    const parsedMessage = message;

    switch (parsedMessage.type) {
      case 'ROOM_LEFT_CONFIRMATION':
        currentRoom = null;
        localStorage.removeItem('currentRoomId');
        displayRooms();
        break;

      case 'COUNTDOWN':
        startCountdownAndRedirect(parseInt(parsedMessage.content));
        break;

      case 'ROOM_UPDATE':
        const updatedRoom = JSON.parse(parsedMessage.content);
        if (currentRoom && currentRoom.id === updatedRoom.id) {
          currentRoom = updatedRoom;
          displayRoomDetails(currentRoom);

          // Проверяем, заполнена ли комната
          if (currentRoom.playerIds.length === currentRoom.maxPlayers) {
            notifyRoomFull(currentRoom.id); // Уведомляем сервер, что комната заполнена
          }
        }
        break;

      case 'ROOM_DISBANDED':
        handleRoomDisbanded();
        break;

      case 'ROOM_LIST_UPDATE':
        rooms = JSON.parse(parsedMessage.content);
        if (!currentRoom) {
          displayRooms();
        }
        break;

      case 'ROOM_JOIN_CONFIRMATION':
        const joinedRoom = JSON.parse(parsedMessage.content);
        currentRoom = joinedRoom;
        localStorage.setItem('currentRoomId', joinedRoom.id);
        displayRoomDetails(currentRoom);
        break;

      default:
        console.log("Неизвестный тип сообщения:", message.type);
    }
  }

  function startCountdownAndRedirect(seconds) {
    showCountdown(seconds);
    const countdownInterval = setInterval(() => {
      seconds -= 1;
      if (seconds <= 0) {
        clearInterval(countdownInterval);
        window.location.href = `/game?roomId=${currentRoom.id}`;
      }
    }, 1000);
  }
  function notifyRoomFull(roomId) {
    if (currentRoom && currentRoom.creatorId === username) {
      const roomFullEvent = {
        type: 'ROOM_FULL',
        content: roomId.toString(),
      };
      roomsSocket.send(JSON.stringify(roomFullEvent)); // Отправляем сообщение на сервер
    }
  }
  function showCountdown(secondsLeft) {
    const countdownElement = document.createElement('div');
    countdownElement.id = 'countdownTimer';

    countdownElement.style.position = 'fixed';
    countdownElement.style.top = '50%';
    countdownElement.style.left = '50%';
    countdownElement.style.transform = 'translate(-50%, -50%)';
    countdownElement.style.fontSize = '72px';
    countdownElement.style.color = '#fff';
    countdownElement.style.backgroundColor = 'rgba(0, 0, 0, 0.7)';
    countdownElement.style.padding = '20px 40px';
    countdownElement.style.borderRadius = '15px';
    countdownElement.style.boxShadow = '0 4px 15px rgba(0, 0, 0, 0.3)';
    countdownElement.style.textAlign = 'center';
    countdownElement.style.zIndex = '1000';

    document.body.appendChild(countdownElement);

    countdownElement.textContent = secondsLeft;

    const countdownInterval = setInterval(() => {
      secondsLeft -= 1;
      countdownElement.textContent = secondsLeft;

      countdownElement.style.transform = 'translate(-50%, -50%) scale(1.2)';
      setTimeout(() => {
        countdownElement.style.transform = 'translate(-50%, -50%) scale(1)';
      }, 100);

      if (secondsLeft <= 0) {
        clearInterval(countdownInterval);
        countdownElement.remove();
      }
    }, 1000);
  }
  function handleRoomDisbanded() {
    currentRoom = null;
    localStorage.removeItem('currentRoomId');
    displayRooms();
  }

  function openCreateRoomModal() {
    document.getElementById('createRoomModal').style.display = 'block';
  }

  function closeCreateRoomModal() {
    document.getElementById('createRoomModal').style.display = 'none';
  }

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
  function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) return parts.pop().split(';').shift();
  }


  function updateRoomTimer() {
    const timerElement = document.getElementById('roomTimer');

    if (currentRoom && timerElement) {
      const now = Date.now();
      const createdAt = new Date(currentRoom.createdAt).getTime();
      const timeLeft = 10 * 60 * 1000 - (now - createdAt);

      if (timeLeft > 0) {
        const minutes = Math.floor(timeLeft / 60000);
        const seconds = Math.floor((timeLeft % 60000) / 1000);
        timerElement.textContent =
        `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;

        setTimeout(updateRoomTimer, 1000);
      } else {
        timerElement.textContent = '00:00';
        disbandRoom(currentRoom.id);
      }
    }
  }

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

    startRoomListTimers();
  }
  function getTimeLeft(createdAt) {
    const now = Date.now();
    const created = new Date(createdAt).getTime();
    const timePassed = now - created;
    const timeLeft = Math.max(0, 10 * 60 * 1000 - timePassed);
    const minutes = Math.floor(timeLeft / (60 * 1000));
    const seconds = Math.floor((timeLeft % (60 * 1000)) / 1000);
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  }



  function startRoomListTimers() {
    setInterval(() => {
      document.querySelectorAll('.room-timer').forEach(timer => {
        const createdAt = parseInt(timer.dataset.created);
        timer.textContent = getTimeLeft(createdAt);
        if (timer.textContent === '00:00') {
          const roomRow = timer.closest('tr');
          if (roomRow) {
            roomRow.remove();
          }
        }
      });
    }, 1000);
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

  async function joinRoom(roomId) {
    if (roomsSocket && roomsSocket.readyState === WebSocket.OPEN) {
      const joinEvent = {
        type: 'ROOM_JOINED',
        sender: username,
        content: roomId.toString()
      };
      roomsSocket.send(JSON.stringify(joinEvent));

      currentRoom = rooms.find(room => room.id == roomId);
      displayRoomDetails(currentRoom);
    } else {
      console.error("WebSocket не подключен. Попытка повторного подключения...");
      connectToRoomsSocket();
    }
  }



  function displayRoomDetails(room) {
    const roomsList = document.getElementById('gameRoomsList');
    if (!room) {
      roomsList.innerHTML = '<p>Вы не находитесь в комнате</p>';
      return;
    }

    roomsList.innerHTML = `
      <h3 class="room-name">Комната: ${room.name}</h3>
      <div class="room-category">Категория: ${room.category}</div>
      <div class="room-players">Игроки: ${room.playerIds.length}/${room.maxPlayers}</div>
      <div id="roomTimer"></div>
      <ul class="player-list">
        ${room.playerIds.map(playerId => `<li class="player-item">${playerId}</li>`).join('')}
      </ul>
      ${room.creatorId === username ?
        `<button class="start-game-btn" id="startGameBtn">Начать игру</button>
         <button class="disband-room-btn" id="disbandRoomBtn">Распустить комнату</button>` :
        `<button class="leave-room-btn" id="leaveRoomBtn">Покинуть комнату</button>`
      }
    `;

    // Hide buttons when inside a room
    const refreshBtn = document.getElementById('refreshRoomsBtn');
    const createRoomBtn = document.getElementById('createRoomBtn');
    const roomHeader = document.getElementsByClassName('game-rooms-header')[0];

    if (refreshBtn && createRoomBtn && roomHeader) {
      refreshBtn.style.display = 'none';
      createRoomBtn.style.display = 'none';
      roomHeader.style.display = 'none';
    }

    if (room.creatorId === username) {
      document.getElementById('startGameBtn').addEventListener('click', () => startGame(room.id));
      document.getElementById('disbandRoomBtn').addEventListener('click', () => disbandRoom(room.id));
    } else {
      document.getElementById('leaveRoomBtn').addEventListener('click', () => leaveRoom(room.id));
    }

    updateRoomTimer();
  }

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

  function leaveRoom(roomId) {
    const event = {
      type: 'ROOM_LEFT',
      content: roomId.toString()
    };
    roomsSocket.send(JSON.stringify(event));
    refreshRooms();
  }

  function refreshRooms() {
    if (roomsSocket && roomsSocket.readyState === WebSocket.OPEN) {
      const refreshEvent = {
        type: 'ROOM_LIST_REQUEST',
        sender: username,
        content: 'refresh'
      };
      roomsSocket.send(JSON.stringify(refreshEvent));
      const savedRoomId = localStorage.getItem('currentRoomId');
      if (savedRoomId) {
        joinRoom(savedRoomId)
        }
    } else if (!roomsSocket || roomsSocket.readyState === WebSocket.CLOSED) {
      console.error("WebSocket не подключен. Попытка повторного подключения...");
      connectToRoomsSocket();
    }
  }
  function initializeGameRooms() {
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