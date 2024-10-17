const playerList = document.getElementById('playerList');
const playerSearch = document.getElementById('playerSearch');
const playerSocket = new WebSocket(`ws://${location.host}/players?username=${username}`);

playerSocket.onmessage = function(event) {
  const data = JSON.parse(event.data);
  if (data.type === 'PLAYER_LIST_UPDATE' || data.type === 'PLAYER_SEARCH_RESULT') {
    updatePlayerList(JSON.parse(data.content));
  }
};

function updatePlayerList(players) {
  playerList.innerHTML = '';
  players.forEach(player => {
    const li = document.createElement('li');
    li.textContent = player.username;
    li.style.color = player.status === 'online' ? 'black' : 'lightgray';
    playerList.appendChild(li);
  });
}

playerSearch.addEventListener('input', function() {
  const searchQuery = this.value;
  playerSocket.send(JSON.stringify({
    type: 'PLAYER_SEARCH',
    content: searchQuery
  }));
});

// Запрос начального списка игроков
playerSocket.onopen = function() {
  playerSocket.send(JSON.stringify({
    type: 'PLAYER_LIST_REQUEST'
  }));
  updatePlayerStatus(); // Add this line

};

// Обновление статуса при подключении/отключении
window.addEventListener('online', updatePlayerStatus);
window.addEventListener('offline', updatePlayerStatus);

function updatePlayerStatus() {
  const status = navigator.onLine ? 'online' : 'offline';
  playerSocket.send(JSON.stringify({
    type: 'PLAYER_STATUS_CHANGE',
    content: status
  }));
}