document.addEventListener('DOMContentLoaded', () => {
  const questSocket = new WebSocket(`ws://${location.host}/quest`);

  questSocket.onopen = () => {
    console.log('WebSocket подключен к /quest');
    requestQuestion();  // Запрашиваем вопрос при подключении
  };

  questSocket.onmessage = (event) => {
    const message = JSON.parse(event.data);
    handleEvent(message);
  };

  questSocket.onclose = (event) => {
    console.log('WebSocket закрыт', event.code, event.reason);
  };

  questSocket.onerror = (error) => {
    console.error('Ошибка WebSocket:', error);
  };

  function handleEvent(event) {
    switch (event.type) {
      case 'QUESTION':
        displayQuestion(JSON.parse(event.content));
        break;
      default:
        console.warn('Неизвестный тип события:', event.type);
    }
  }

  function displayQuestion(question) {
    const questionContainer = document.getElementById('question-container');
    questionContainer.innerHTML = `
      <img src="${question.imageUrl}" alt="Question Image" class="question-image" />
      <h3>${question.questionText}</h3>
      <div class="options">
        <button onclick="sendAnswer('A')">${question.optionA}</button>
        <button onclick="sendAnswer('B')">${question.optionB}</button>
        <button onclick="sendAnswer('C')">${question.optionC}</button>
        <button onclick="sendAnswer('D')">${question.optionD}</button>
      </div>
    `;
  }

  function requestQuestion() {
    const event = { type: 'GET_QUESTION', content: null, sender: 'client' };
    questSocket.send(JSON.stringify(event));
  }

  window.sendAnswer = function(answer) {
    const event = { type: 'ANSWER', content: answer, sender: 'client' };
    questSocket.send(JSON.stringify(event));
  };
});